/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import java.util.List;

/**
 * Shared base for the "SimpleBraced" family (RDD_KEY_190): brace/bracket-delimited formats with no
 * imperative control flow -- JSON/JSON5 ({@link FormatterJson}) and, eventually, CSS. Holds
 * {@link #padKeysForColonAlignment}, the group-column-padding computation shared by
 * STYLE_DATA_FORMATS.md §1.1 (JSON key/value colon alignment) and §3.1 (CSS property/value colon
 * alignment) -- both describe the identical "pad so `:` lines up, space always precedes `:`"
 * shape.
 */
public abstract class FormatterSimpleBraced extends FormatterCore {

    protected FormatterSimpleBraced(final Lang lang) {
        super(lang);
    }

    /** Returns, for each key in {@code keys} (a single alignment group), the padding spaces to
     *  insert between that key and its `:` so every `:` in the group lines up at the same column
     *  -- the widest key in the group gets exactly one space, every other key gets enough extra
     *  padding to match. */
    public static String[] padKeysForColonAlignment(final List<String> keys) {
        int widest = 0;
        for (final String key : keys) {
            widest = Math.max(widest, key.length());
        }
        final String[] padded = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            final int spaces = widest - keys.get(i).length() + 1;
            final StringBuilder sb = new StringBuilder();
            for (int s = 0; s < spaces; s++) {
                sb.append(' ');
            }
            padded[i] = sb.toString();
        }
        return padded;
    }

    /** Lightweight `normalize-comment-start-case` for the SimpleBraced family: capitalizes the
     *  first letter of {@code commentText} if -- and only if -- the very first non-whitespace
     *  character after the `//`/`/*` delimiter is a lowercase letter. Unlike the curly family's
     *  {@code MiscRuleCore.capitalizeFirstLetter}, this has no keyword-exclusion list or
     *  classifier gate -- JSON/CSS have no language keywords a comment could start with that
     *  would need protecting from titlecasing. Only the very first line of a multi-line block
     *  comment is affected: {@code content.length()} scanning stops at the first letter found. */
    public static String capitalizeCommentStart(final String commentText) {
        final int delimLen = commentText.startsWith("//") || commentText.startsWith("/*") ? 2 : 0;
        int i = delimLen;
        while (i < commentText.length() && commentText.charAt(i) == ' ') {
            i++;
        }
        if (i < commentText.length()) {
            final char c = commentText.charAt(i);
            if (Character.isLetter(c) && Character.isLowerCase(c)) {
                return commentText.substring(0, i) + Character.toUpperCase(c) + commentText.substring(i + 1);
            }
        }
        return commentText;
    }

    /** Reindents a (possibly multi-line) block comment's continuation lines to the new structural
     *  {@code indentPrefix}: the first line is left as-is (the caller already prints
     *  {@code indentPrefix} before it), and every subsequent line gets {@code indentPrefix}
     *  prepended in front of whatever whitespace it already has -- preserving the comment's
     *  original *relative* indentation (e.g. an aligned {@code *} continuation, or hanging
     *  sentence indent) rather than the absolute column it happened to sit at in the source. */
    public static String reindentBlockComment(final String commentText, final String indentPrefix) {
        if (commentText.indexOf('\n') < 0) {
            return commentText;
        }
        final String[] lines = commentText.split("\n", -1);
        final StringBuilder sb = new StringBuilder(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            // If this line already starts with exactly indentPrefix (e.g. re-formatting
            // already-formatted output at the same depth), strip it first so re-adding it below
            // doesn't double up -- this is what makes the operation idempotent.
            final String line = lines[i].startsWith(indentPrefix) ? lines[i].substring(indentPrefix.length())
                    : lines[i];
            sb.append('\n').append(indentPrefix).append(line);
        }
        return sb.toString();
    }
}
