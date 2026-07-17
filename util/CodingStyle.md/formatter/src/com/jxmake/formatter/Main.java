/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    private enum OutputMode { IN_PLACE, DIFF, CHECK, OUT_DIR }

    /**
     * Sentinel {@code run()} result meaning "a fresh server was started -- its live,
     * non-daemon HTTP listener threads must keep this process alive until {@code /shutdown}
     * later calls {@code System.exit} itself." {@code main()} must not call {@code System.exit}
     * in this case, or it would tear the just-started server down immediately. Distinct from
     * every real exit code (0/1/2), which are all non-negative.
     */
    private static final int SERVER_STARTED_KEEP_ALIVE = -1;

    private Main() {
    }

    public static void main(final String[] args) {
        final int code = run(args);
        if (code != SERVER_STARTED_KEEP_ALIVE) {
            System.exit(code);
        }
    }

    static int run(final String[] args) {
        boolean serverMode = false;
        boolean stopMode = false;
        boolean standalone = false;
        boolean formatOff = false;
        OutputMode outputMode = OutputMode.IN_PLACE;
        String outDir = null;
        Integer port = null;
        String explicitLanguage = null;
        boolean preserveTree = false;
        String rootDir = null;
        final List<String> files = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if ("--server".equals(arg)) {
                serverMode = true;
            } else if ("--stop".equals(arg)) {
                stopMode = true;
            } else if ("--standalone".equals(arg)) {
                standalone = true;
            } else if ("--format-off".equals(arg)) {
                formatOff = true;
            } else if ("--diff".equals(arg)) {
                if (outputMode != OutputMode.IN_PLACE) {
                    return usageError("--diff cannot be combined with --check or --out");
                }
                outputMode = OutputMode.DIFF;
            } else if ("--check".equals(arg)) {
                if (outputMode != OutputMode.IN_PLACE) {
                    return usageError("--check cannot be combined with --diff or --out");
                }
                outputMode = OutputMode.CHECK;
            } else if ("--out".equals(arg)) {
                if (outputMode != OutputMode.IN_PLACE) {
                    return usageError("--out cannot be combined with --diff or --check");
                }
                if (i + 1 >= args.length) {
                    return usageError("--out requires a directory argument");
                }
                outputMode = OutputMode.OUT_DIR;
                outDir = args[++i];
            } else if ("--port".equals(arg)) {
                if (i + 1 >= args.length) {
                    return usageError("--port requires a numeric argument");
                }
                final String portArg = args[++i];
                try {
                    port = Integer.valueOf(Integer.parseInt(portArg));
                } catch (final NumberFormatException e) {
                    return usageError("--port requires a numeric argument, got: " + portArg);
                }
            } else if ("--lang".equals(arg)) {
                if (i + 1 >= args.length) {
                    return usageError("--lang requires an argument (" + Lang.SUPPORTED_LANGUAGES + ", "
                            + Lang.SCAFFOLD_ONLY_LANGUAGES + ")");
                }
                final String langArg = args[++i];
                if (!Lang.isRecognized(langArg)) {
                    return usageError("--lang must be one of: " + Lang.SUPPORTED_LANGUAGES + ", "
                            + Lang.SCAFFOLD_ONLY_LANGUAGES + " (got: " + langArg + ")");
                }
                explicitLanguage = langArg;
            } else if ("--preserve-tree".equals(arg)) {
                preserveTree = true;
            } else if ("--root".equals(arg)) {
                if (i + 1 >= args.length) {
                    return usageError("--root requires a directory argument");
                }
                rootDir = args[++i];
            } else if (arg.startsWith("--")) {
                return usageError("unknown flag: " + arg);
            } else {
                files.add(arg);
            }
        }

        if (serverMode && stopMode) {
            return usageError("--server and --stop cannot be combined");
        }
        if ((serverMode || stopMode) && !files.isEmpty()) {
            return usageError("--server/--stop do not take file arguments");
        }
        if ((serverMode || stopMode) && explicitLanguage != null) {
            return usageError("--server/--stop do not take --lang");
        }
        if (preserveTree && outputMode != OutputMode.OUT_DIR) {
            return usageError("--preserve-tree requires --out DIR");
        }
        if (preserveTree && rootDir == null) {
            return usageError("--preserve-tree requires --root DIR");
        }
        if (rootDir != null && !preserveTree) {
            return usageError("--root DIR has no effect without --preserve-tree");
        }

        final Map<String, String> cliOverrides = new LinkedHashMap<String, String>();
        if (port != null) {
            cliOverrides.put("server-port", String.valueOf(port));
        }

        if (serverMode) {
            final Config config = Config.resolve(Paths.get(".").toAbsolutePath(), cliOverrides);
            return ServerMode.start(config) ? SERVER_STARTED_KEEP_ALIVE : 0;
        }
        if (stopMode) {
            return ServerMode.stop() ? 0 : 1;
        }

        if (files.isEmpty()) {
            return usageError("no input files given");
        }

        boolean anyChanged = false;
        boolean anyError = false;
        for (final String file : files) {
            try {
                if (processFile(Paths.get(file), outputMode, outDir, standalone, formatOff, cliOverrides,
                        explicitLanguage, preserveTree, rootDir)) {
                    anyChanged = true;
                }
            } catch (final IOException e) {
                System.err.println("jxmake-code-formatter: error: " + file + ": " + e.getMessage());
                anyError = true;
            } catch (final Exception e) {
                System.err.println("jxmake-code-formatter: internal error: " + file + ": " + e);
                e.printStackTrace();
                anyError = true;
            }
        }
        if (anyError) {
            return 1;
        }
        if (outputMode == OutputMode.CHECK && anyChanged) {
            return 1;
        }
        return 0;
    }

    private static int usageError(final String message) {
        System.err.println("jxmake-code-formatter: error: " + message);
        printUsage();
        return 2;
    }

    private static void printUsage() {
        System.err.println("usage: jxmake-code-formatter [--standalone] [--format-off] "
                + "[--lang " + Lang.SUPPORTED_LANGUAGES.replace(", ", "|") + "|"
                + Lang.SCAFFOLD_ONLY_LANGUAGES.replace(", ", "|") + "] "
                + "[--diff | --check | --out DIR [--preserve-tree --root DIR]] [file...]");
        System.err.println("       jxmake-code-formatter --server [--port N]");
        System.err.println("       jxmake-code-formatter --stop");
    }

    /** Returns {@code true} if the file's formatted content differs from its original content. */
    private static boolean processFile(final Path path, final OutputMode outputMode, final String outDir,
            final boolean standalone, final boolean formatOff, final Map<String, String> cliOverrides,
            final String explicitLanguage, final boolean preserveTree, final String rootDir) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("no such file: " + path);
        }
        final String language = explicitLanguage != null ? explicitLanguage : inferLanguage(path);
        if (language == null) {
            throw new IOException("could not infer language from file extension: " + path
                    + " (use --lang c|cpp|java to override)");
        }
        if (Lang.isScaffoldOnly(language)) {
            throw new UnsupportedLanguageException(language);
        }

        final String original = readFile(path);
        final String formatted = format(path, language, original, standalone, formatOff, cliOverrides);
        final boolean changed = !formatted.equals(original);

        switch (outputMode) {
            case DIFF:
                if (changed) {
                    System.out.print(unifiedDiff(path.toString(), original, formatted));
                }
                break;
            case CHECK:
                break;
            case OUT_DIR:
                final Path outPath;
                if (preserveTree) {
                    final Path absRoot = Paths.get(rootDir).toAbsolutePath().normalize();
                    final Path absPath = path.toAbsolutePath().normalize();
                    if (!absPath.startsWith(absRoot)) {
                        throw new IOException("file is not under --root " + rootDir + ": " + path);
                    }
                    outPath = Paths.get(outDir).resolve(absRoot.relativize(absPath));
                } else {
                    outPath = Paths.get(outDir).resolve(path.getFileName());
                }
                final Path outParent = outPath.toAbsolutePath().getParent();
                if (outParent != null) {
                    Files.createDirectories(outParent);
                }
                writeFile(outPath, formatted);
                break;
            case IN_PLACE:
            default:
                if (changed) {
                    writeFile(path, formatted);
                }
                break;
        }

        return changed;
    }

    private static String format(final Path path, final String language, final String original,
            final boolean standalone, final boolean formatOff, final Map<String, String> cliOverrides)
            throws IOException {
        if (!standalone) {
            final int serverPort = ServerMode.findRunningServerPort();
            if (serverPort > 0) {
                try {
                    return delegateToServer(serverPort, path, language, original, formatOff);
                } catch (final IOException e) {
                    System.err.println("jxmake-code-formatter: warning: server delegation failed (" + e.getMessage()
                            + "), falling back to standalone formatting");
                }
            }
        }
        return formatStandalone(path, language, original, formatOff, cliOverrides);
    }

    private static String formatStandalone(final Path path, final String language, final String original,
            final boolean formatOff, final Map<String, String> baseCliOverrides) throws IOException {
        final Map<String, String> inFileOverrides = InFileConfig.parse(original);
        Config config = Config.resolve(path, baseCliOverrides, inFileOverrides);
        if ("auto".equals(config.indentStyle())) {
            final String resolvedStyle = resolveAutoIndentStyle(path);
            final Map<String, String> merged = new LinkedHashMap<String, String>(baseCliOverrides);
            merged.put("indent-style", resolvedStyle);
            config = Config.resolve(path, merged, inFileOverrides);
        }
        final String formatted = FormatterCore.forLanguage(language)
                .formatOne(original, path.toString(), config, formatOff);
        return applyLineEndings(formatted, original, config.lineEndings());
    }

    /**
     * Standalone-mode persistent cache for {@code IndentationDetector.detect()}, layered on top
     * of that class's own in-memory, per-call cache (which only lives for one JVM invocation and
     * is therefore useless across separate CLI runs). Key = SHA-256 hex of the boundary
     * directory's absolute path string; cache file = {@code /tmp/jxmake-code-formatter-indent-<key>.cache};
     * content = detected style + newline + boundary dir's {@code lastModified} epoch ms. A
     * mismatch (someone added/removed a source file under the boundary dir since the last scan)
     * invalidates the entry and triggers a fresh scan.
     */
    private static String resolveAutoIndentStyle(final Path path) throws IOException {
        final Path fileDir = path.toAbsolutePath().getParent();
        final Path boundaryDir = IndentationDetector.findBoundaryDir(fileDir);
        final long boundaryLastModified = Files.exists(boundaryDir) ? Files.getLastModifiedTime(boundaryDir)
                .toMillis() : 0L;
        final Path cacheFile = Paths.get("/tmp", "jxmake-code-formatter-indent-" + sha256Hex(boundaryDir.toString())
                + ".cache");

        if (Files.isRegularFile(cacheFile)) {
            final List<String> lines = Files.readAllLines(cacheFile);
            if (lines.size() >= 2) {
                try {
                    if (Long.parseLong(lines.get(1).trim()) == boundaryLastModified) {
                        final Map<Path, String> primed = new HashMap<Path, String>();
                        primed.put(fileDir, lines.get(0).trim());
                        return IndentationDetector.detect(fileDir, primed);
                    }
                } catch (final NumberFormatException ignored) {
                    // fall through to rescan below
                }
            }
            try {
                Files.deleteIfExists(cacheFile);
            } catch (final IOException e) {
                System.err.println("jxmake-code-formatter: warning: could not delete stale indent-style cache: "
                        + e.getMessage());
            }
        }

        final String detected = IndentationDetector.detect(fileDir, new HashMap<Path, String>());
        try {
            Files.write(cacheFile, (detected + "\n" + boundaryLastModified + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            System.err.println("jxmake-code-formatter: warning: could not write indent-style cache: " + e.getMessage());
        }
        return detected;
    }

    private static String sha256Hex(final String input) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder();
            for (final byte b : hash) {
                sb.append(String.format("%02x", Byte.valueOf(b)));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is guaranteed available on every JDK", e);
        }
    }

    private static String applyLineEndings(final String formatted, final String original,
            final String lineEndingsConfig) {
        final String targetEnding;
        if ("crlf".equals(lineEndingsConfig)) {
            targetEnding = "\r\n";
        } else if ("preserve".equals(lineEndingsConfig)) {
            targetEnding = detectDominantLineEnding(original);
        } else {
            targetEnding = "\n";
        }
        return "\n".equals(targetEnding) ? formatted : formatted.replace("\n", targetEnding);
    }

    private static String detectDominantLineEnding(final String text) {
        int crlf = 0;
        int lf = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                if (i > 0 && text.charAt(i - 1) == '\r') {
                    crlf++;
                } else {
                    lf++;
                }
            }
        }
        return crlf > lf ? "\r\n" : "\n";
    }

    private static String delegateToServer(final int port, final Path path, final String language,
            final String content, final boolean formatOff) throws IOException {
        final String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
        final URL url = new URL("http://localhost:" + port + "/format?path=" + encodedPath + "&lang=" + language
                + (formatOff ? "&format-off=true" : ""));
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(500);
        connection.setReadTimeout(10000);
        try (final OutputStream out = connection.getOutputStream()) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        final int status = connection.getResponseCode();
        final InputStream responseStream = status >= 200 && status < 300 ? connection.getInputStream()
                : connection.getErrorStream();
        final String body = readStream(responseStream);
        connection.disconnect();
        if (status < 200 || status >= 300) {
            throw new IOException("server returned " + status + ": " + body);
        }
        return body;
    }

    private static String readStream(final InputStream in) throws IOException {
        final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        final byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String inferLanguage(final Path path) {
        return Lang.infer(path.getFileName().toString());
    }

    private static String readFile(final Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void writeFile(final Path path, final String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    // ---- unified diff (--diff) ----

    private static final class DiffRun {
        final char type;
        final int aStart;
        final int aCount;
        final int bStart;
        final int bCount;

        DiffRun(final char type, final int aStart, final int aCount, final int bStart, final int bCount) {
            this.type = type;
            this.aStart = aStart;
            this.aCount = aCount;
            this.bStart = bStart;
            this.bCount = bCount;
        }
    }

    private static List<String> splitLines(final String text) {
        final List<String> lines = new ArrayList<String>(Arrays.asList(text.split("\n", -1)));
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    /**
     * Line-level LCS diff (O(n*m) dynamic programming -- acceptable for source-file-sized
     * inputs). Produces a minimal sequence of equal/delete/insert runs; {@link #unifiedDiff}
     * then wraps the span between the first and last change in a single hunk with clamped
     * leading/trailing context, deliberately not splitting widely-separated change clusters into
     * multiple hunks the way GNU diff does -- simpler and still fully correct, just less terse
     * for files with several far-apart edits.
     */
    private static List<DiffRun> computeDiffRuns(final List<String> a, final List<String> b) {
        final int n = a.size();
        final int m = b.size();
        final int[][] dp = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                dp[i][j] = a.get(i).equals(b.get(j)) ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }

        final List<DiffRun> steps = new ArrayList<DiffRun>();
        int i = 0;
        int j = 0;
        while (i < n || j < m) {
            if (i < n && j < m && a.get(i).equals(b.get(j))) {
                steps.add(new DiffRun(' ', i, 1, j, 1));
                i++;
                j++;
            } else if (j < m && (i == n || dp[i][j + 1] >= dp[i + 1][j])) {
                steps.add(new DiffRun('+', i, 0, j, 1));
                j++;
            } else {
                steps.add(new DiffRun('-', i, 1, j, 0));
                i++;
            }
        }

        final List<DiffRun> runs = new ArrayList<DiffRun>();
        for (final DiffRun step : steps) {
            if (!runs.isEmpty() && runs.get(runs.size() - 1).type == step.type) {
                final DiffRun prev = runs.remove(runs.size() - 1);
                runs.add(new DiffRun(step.type, prev.aStart, prev.aCount + step.aCount, prev.bStart,
                        prev.bCount + step.bCount));
            } else {
                runs.add(step);
            }
        }
        return runs;
    }

    private static String unifiedDiff(final String label, final String original, final String formatted) {
        final List<String> a = splitLines(original);
        final List<String> b = splitLines(formatted);
        final List<DiffRun> runs = computeDiffRuns(a, b);

        int firstChange = -1;
        int lastChange = -1;
        for (int idx = 0; idx < runs.size(); idx++) {
            if (runs.get(idx).type != ' ') {
                if (firstChange < 0) {
                    firstChange = idx;
                }
                lastChange = idx;
            }
        }
        if (firstChange < 0) {
            return "";
        }

        final int context = 3;
        final List<DiffRun> hunk = new ArrayList<DiffRun>();

        final int aHunkStart;
        final int bHunkStart;
        if (firstChange > 0) {
            final DiffRun leadEqual = runs.get(firstChange - 1);
            final int take = Math.min(context, leadEqual.aCount);
            final int skip = leadEqual.aCount - take;
            aHunkStart = leadEqual.aStart + skip;
            bHunkStart = leadEqual.bStart + skip;
            if (take > 0) {
                hunk.add(new DiffRun(' ', aHunkStart, take, bHunkStart, take));
            }
        } else {
            aHunkStart = runs.get(firstChange).aStart;
            bHunkStart = runs.get(firstChange).bStart;
        }

        for (int idx = firstChange; idx <= lastChange; idx++) {
            hunk.add(runs.get(idx));
        }

        if (lastChange < runs.size() - 1) {
            final DiffRun trailEqual = runs.get(lastChange + 1);
            final int take = Math.min(context, trailEqual.aCount);
            if (take > 0) {
                hunk.add(new DiffRun(' ', trailEqual.aStart, take, trailEqual.bStart, take));
            }
        }

        int aCount = 0;
        int bCount = 0;
        for (final DiffRun r : hunk) {
            aCount += r.aCount;
            bCount += r.bCount;
        }

        final StringBuilder out = new StringBuilder();
        out.append("--- ").append(label).append("\n");
        out.append("+++ ").append(label).append(" (formatted)\n");
        out.append("@@ -").append(aHunkStart + 1).append(",").append(aCount)
                .append(" +").append(bHunkStart + 1).append(",").append(bCount).append(" @@\n");
        for (final DiffRun r : hunk) {
            if (r.type == ' ') {
                for (int k = 0; k < r.aCount; k++) {
                    out.append(' ').append(a.get(r.aStart + k)).append("\n");
                }
            } else if (r.type == '-') {
                for (int k = 0; k < r.aCount; k++) {
                    out.append('-').append(a.get(r.aStart + k)).append("\n");
                }
            } else {
                for (int k = 0; k < r.bCount; k++) {
                    out.append('+').append(b.get(r.bStart + k)).append("\n");
                }
            }
        }
        return out.toString();
    }
}
