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

    protected FormatterSimpleBraced(final Lang lang)
    {
        super(lang);
    }

    /**
     * Returns, for each key in {@code keys} (a single alignment group), the padding spaces to
     *  insert between that key and its `:` so every `:` in the group lines up at the same column
     *  -- the widest key in the group gets exactly one space, every other key gets enough extra
     *  padding to match
     */
    public static String[] padKeysForColonAlignment(final List<String> keys)
    {
        int widest = 0;
        for(final String key : keys) widest = Math.max( widest, key.length() );
        final String[] padded = new String[ keys.size() ];
        for( int i = 0; i < keys.size(); ++i ) {
            final int           spaces = widest - keys.get(i).length() + 1;
            final StringBuilder sb     = new StringBuilder();
            for(int s = 0; s < spaces; ++s) sb.append(' ');
            padded[i] = sb.toString();
        }

        return padded;
    }

    /**
     * Lightweight `normalize-comment-start-case` for the SimpleBraced family: capitalizes the
     *  first letter of {@code commentText} if -- and only if -- the very first non-whitespace
     *  character after the `//`/`/*` delimiter is a lowercase letter. Unlike the curly family's
     *  {@code MiscRuleCore.capitalizeFirstLetter}, this has no keyword-exclusion list or
     *  classifier gate -- JSON/CSS have no language keywords a comment could start with that
     *  would need protecting from titlecasing. Only the very first line of a multi-line block
     *  comment is affected: {@code content.length()} scanning stops at the first letter found.
     *  <p><b>Directive-comment carve-out</b> (found via real-code dogfood testing against
     *  `twbs/bootstrap`): a single-line comment whose entire trimmed body contains no whitespace
     *  at all (e.g. CSS's `/* rtl:begin:ignore *&#47;`/`/* rtl:end:ignore *&#47;` -- a case-sensitive
     *  rtlcss build-tool directive, or `/* stylelint-disable *&#47;`) is treated as an opaque
     *  machine-readable token, not an English prose sentence, and is never capitalized -- doing so
     *  would silently break the third-party tool that parses it. This is narrower than "starts with
     *  a lowercase word containing a colon" (which would wrongly suppress the common `TODO: fix
     *  this` / `NOTE: ...` prose convention, already capitalized as-is since `TODO`/`NOTE` start
     *  uppercase): only a body with *zero* whitespace anywhere is treated as directive-like.
     */
    public static String capitalizeCommentStart(final String commentText)
    {
        final int delimLen = commentText.startsWith("//") || commentText.startsWith("/*") ? 2 : 0;
              int i        = delimLen;
        while( i < commentText.length() && commentText.charAt(i) == ' ' ) i++;
        if( i < commentText.length() ) {
            final char c = commentText.charAt(i);
            if( Character.isLetter(
                c
            ) && Character.isLowerCase(
                c
            ) && !isSingleTokenDirective(
                commentText, i
            ) ) return commentText.substring(
                0, i
            ) + Character.toUpperCase(
                c
            ) + commentText.substring(
                i + 1
            );
        } // if

        return commentText;
    }

    /**
     * True iff the first line's entire body (starting at {@code bodyStart}, the first
     *  non-whitespace character after the delimiter, up to end-of-line or the comment's closing
     *  `*&#47;`/end-of-string) is a *single* whitespace-free token containing a `:` or `-`
     *  separator -- i.e. the whole comment line is one opaque directive like `rtl:begin:ignore` or
     *  `stylelint-disable`, not a prose sentence that merely happens to start with a
     *  hyphenated/colon-containing word (e.g. "auto-generated file, do not edit" has more content
     *  after the first token and must NOT be treated as directive-like).
     */
    private static boolean isSingleTokenDirective(final String commentText, final int bodyStart)
    {
        int end = bodyStart;
        while( end < commentText.length() ) {
            final char c = commentText.charAt(end);
            if( c == '\n' || Character.isWhitespace(c) ) break;
            if( c == '*' && end + 1 < commentText.length() && commentText.charAt(
                end + 1
            ) == '/' ) break;
            ++end;
        } // while
        // Everything from `end` to line-end/comment-close must be pure trailing whitespace (plus
        // the closing `*&#47;`, if any) -- otherwise more sentence content follows the first token
        // and this isn't a single-token directive comment
        int rest = end;
        while( rest < commentText.length() ) {
            final char c = commentText.charAt(rest);
            if(c == '\n') break;
            if( c == '*' && rest + 1 < commentText.length() && commentText.charAt(
                rest + 1
            ) == '/' ) break;
            if( !Character.isWhitespace(c) ) return false;
            ++rest;
        } // while
        final String token = commentText.substring(bodyStart, end);

        return token.indexOf(':') >= 0 || token.indexOf('-') >= 0;
    }

    /**
     * Reindents a (possibly multi-line) block comment's continuation lines to the new structural
     *  {@code indentPrefix}: the first line is left as-is (the caller already prints
     *  {@code indentPrefix} before it), and every subsequent line gets {@code indentPrefix}
     *  prepended in front of whatever whitespace it already has -- preserving the comment's
     *  original *relative* indentation (e.g. an aligned {@code *} continuation, or hanging
     *  sentence indent) rather than the absolute column it happened to sit at in the source.
     */
    public static String reindentBlockComment(final String commentText, final String indentPrefix)
    {
        if( commentText.indexOf('\n') < 0 ) return commentText;
        final String[]      lines = commentText.split("\n", -1);
        final StringBuilder sb    = new StringBuilder( lines[0] );
        for(int i = 1; i < lines.length; ++i) {
            // If this line already starts with exactly indentPrefix (e.g. re-formatting
            // already-formatted output at the same depth), strip it first so re-adding it below
            // doesn't double up -- this is what makes the operation idempotent.
            final String line = lines[i].startsWith(
                indentPrefix
            ) ? lines[i].substring(
                indentPrefix.length()
            ) : lines[i];
            sb.append('\n').append(indentPrefix).append(line);
        } // for

        return sb.toString();
    }

} // class FormatterSimpleBraced
