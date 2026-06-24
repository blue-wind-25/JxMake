/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class IndentationDetector {
    private static final String[] SOURCE_EXTENSIONS = {
        ".java", ".c", ".h", ".cpp", ".cc", ".cxx", ".hh", ".hpp", ".hxx"
    };
    private static final int SAMPLE_FILE_CAP = 10;
    private static final String[] BOUNDARY_MARKERS = { ".style-fmt", ".git", ".hg" };

    private IndentationDetector() {
    }

    public static String detect(final Path fileDir, final Map<Path, String> cache) {
        final String cached = cache.get(fileDir);
        if (cached != null) {
            return cached;
        }
        final Path boundaryDir = findBoundaryDir(fileDir);
        final String detected = sampleDir(boundaryDir);
        cache.put(fileDir, detected);
        return detected;
    }

    public static String detectFromContent(final String source) {
        try (final BufferedReader reader = new BufferedReader(new StringReader(source))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String style = styleOfIndentedLine(line);
                if (style != null) {
                    return style;
                }
            }
        } catch (final IOException e) {
            return Config.DEFAULT_INDENT_STYLE;
        }
        return Config.DEFAULT_INDENT_STYLE;
    }

    /**
     * Exposed (not just used internally by {@link #detect}) so {@code Main}'s standalone-mode
     * temp-file cache can key on the same boundary directory this class would otherwise compute
     * and immediately discard -- the cache must invalidate on the boundary dir's
     * {@code lastModified}, which only means anything if it is the *same* directory `detect`
     * itself would have scanned. Purely additive: behavior and signature for the one existing
     * caller ({@link #detect}) are unchanged.
     */
    public static Path findBoundaryDir(final Path startDir) {
        final Path home = Paths.get(System.getProperty("user.home"));
        Path dir = startDir;
        while (dir != null) {
            for (final String marker : BOUNDARY_MARKERS) {
                if (Files.exists(dir.resolve(marker))) {
                    return dir;
                }
            }
            if (dir.equals(home)) {
                return home;
            }
            dir = dir.getParent();
        }
        return home;
    }

    private static String sampleDir(final Path boundaryDir) {
        int spacesVotes = 0;
        int tabsVotes = 0;
        int filesSignaled = 0;

        try (final Stream<Path> walk = Files.walk(boundaryDir)) {
            for (final Path path : (Iterable<Path>) walk::iterator) {
                if (filesSignaled >= SAMPLE_FILE_CAP) {
                    break;
                }
                if (!Files.isRegularFile(path) || !hasSourceExtension(path)) {
                    continue;
                }
                final String vote = voteForFile(path);
                if (vote == null) {
                    continue;
                }
                filesSignaled++;
                if ("spaces".equals(vote)) {
                    spacesVotes++;
                } else if ("tabs".equals(vote)) {
                    tabsVotes++;
                }
            }
        } catch (final IOException e) {
            return Config.DEFAULT_INDENT_STYLE;
        }

        if (spacesVotes > tabsVotes) {
            return "spaces";
        }
        if (tabsVotes > spacesVotes) {
            return "tabs";
        }
        return Config.DEFAULT_INDENT_STYLE;
    }

    private static boolean hasSourceExtension(final Path path) {
        final String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (final String ext : SOURCE_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static String voteForFile(final Path path) {
        try (final BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String style = styleOfIndentedLine(line);
                if (style != null) {
                    return style;
                }
            }
        } catch (final IOException e) {
            return null;
        }
        return null;
    }

    private static String styleOfIndentedLine(final String line) {
        if (line.isEmpty()) {
            return null;
        }
        final char first = line.charAt(0);
        if (first == ' ') {
            return "spaces";
        }
        if (first == '\t') {
            return "tabs";
        }
        return null;
    }
}
