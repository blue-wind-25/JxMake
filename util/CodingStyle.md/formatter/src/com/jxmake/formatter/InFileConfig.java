/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Raw-text scanner for the top-of-file {@code JXM_CFMT_CFG} directive (RDD_KEY_167,
 * STATE_COMMON.md's "In-file Config Support"). Runs on the raw source text before {@link
 * Config#resolve}/{@code Formatter} construction, since config values are baked into rule
 * objects before tokenizing starts. When present, its values are the highest-priority config
 * layer -- they override file-based {@code .jxmake-code-formatter}, env vars, CLI flags, AND
 * the server's own inline query-param config, all for the same key.
 */
public final class InFileConfig {

    private static final Pattern DIRECTIVE = Pattern.compile(
            "//%\\s*JXM_CFMT_CFG\\s+([^\\r\\n]*)|/\\*%\\s*JXM_CFMT_CFG\\s+(.*?)\\s*\\*/", Pattern.DOTALL);

    // Consumes a run of leading blank lines and whole comments (// or /* */) from the start of
    // a file -- the "top-of-file preamble" a JXM_CFMT_CFG directive is allowed to live in.
    private static final Pattern PREAMBLE_PIECE = Pattern.compile(
            "\\G(?:[ \\t]*\\r?\\n|[ \\t]*//[^\\r\\n]*(?:\\r?\\n|$)|[ \\t]*/\\*.*?\\*/[ \\t]*)", Pattern.DOTALL);

    private InFileConfig() {
    }

    /**
     * Parses {@code source} for a {@code JXM_CFMT_CFG} directive and returns its key/value
     * overrides (empty map if none present). Throws {@link IOException} for: more than one
     * directive occurrence anywhere in the file, a directive found past the top-of-file
     * comment/blank-line preamble, or a directive naming a key that either isn't a recognized
     * config key or is {@code server-port} (a process-wide property, not a per-file one).
     */
    public static Map<String, String> parse(final String source) throws IOException {
        final Matcher m = DIRECTIVE.matcher(source);
        int count = 0;
        int matchStart = -1;
        String body = null;
        while (m.find()) {
            count++;
            if (count == 1) {
                matchStart = m.start();
                body = m.group(1) != null ? m.group(1) : m.group(2);
            }
        }
        if (count == 0) {
            return java.util.Collections.emptyMap();
        }
        if (count > 1) {
            throw new IOException("multiple JXM_CFMT_CFG directives found -- only one is allowed per file");
        }

        final int preambleEnd = preambleEnd(source);
        if (matchStart >= preambleEnd) {
            throw new IOException("JXM_CFMT_CFG must appear before the first non-comment/non-blank line "
                    + "of the file (move it into the top-of-file comment/blank-line preamble)");
        }

        final Map<String, String> result = new LinkedHashMap<String, String>();
        for (final String piece : body.split(";")) {
            final String trimmed = piece.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            final int eq = trimmed.indexOf('=');
            if (eq < 0) {
                throw new IOException("malformed JXM_CFMT_CFG entry (expected key=value): \"" + trimmed + "\"");
            }
            final String key = trimmed.substring(0, eq).trim();
            final String value = trimmed.substring(eq + 1).trim();
            if (!isPerFileApplicable(key)) {
                throw new IOException("JXM_CFMT_CFG: \"" + key
                        + "\" is not a valid per-file config key (either unrecognized, or 'server-port', which is "
                        + "a process-wide property and cannot be set per-file)");
            }
            result.put(key, value);
        }
        return result;
    }

    private static boolean isPerFileApplicable(final String key) {
        return Config.isKnownKey(key) && !"server-port".equals(key);
    }

    private static int preambleEnd(final String source) {
        final Matcher m = PREAMBLE_PIECE.matcher(source);
        int pos = 0;
        while (pos < source.length() && m.find(pos)) {
            if (m.end() == pos) {
                break;
            }
            pos = m.end();
        }
        return pos;
    }
}
