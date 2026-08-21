/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter;

/**
 * Whole-file, language-aware post-passes for three cross-cutting "Behavior" flags:
 * {@code append-new-line-at-eof}, {@code remove-trailing-spaces}, and
 * {@code normalize-invisible-invalid-unicode}. Runs on the final formatted text, after
 * {@code Main.applyLineEndings} -- purely textual, no tokenizer/AST dependency, mirroring the
 * "COMMON/ALL" cross-cutting scope these flags share (see RDD_LOG.md's entry for this feature).
 */
final class UnicodeAndWhitespaceNormalizer {

    private UnicodeAndWhitespaceNormalizer()
    {
    }

    // ------------------------------------------------------------------------------------------
    // append-new-line-at-eof
    // ------------------------------------------------------------------------------------------

    static String appendNewLineAtEof(final String text)
    {
        if( text.isEmpty() ) return text;
        return text.endsWith("\n") ? text : text + "\n";
    }

    // ------------------------------------------------------------------------------------------
    // remove-trailing-spaces
    // ------------------------------------------------------------------------------------------

    /**
     * Strips trailing spaces/tabs from every line, except a Makefile recipe line (a line whose
     * first character is a literal tab when {@code language} is {@code "makefile"}) -- Make's
     * recipe body is whitespace-sensitive and this flag is defined as a no-op there. Also leaves
     * alone any line inside a raw multi-line string/template-literal body for JS/TS (backtick
     * template literals) and Python (triple-quoted strings) -- see the class doc / RDD_LOG entry:
     * trailing whitespace there could be semantically part of the literal's content, so this
     * flag deliberately does not touch a line once it's known to be inside one of those raw
     * multi-line literal spans (documented known limitation, not attempted).
     */
    static String stripTrailingSpaces(final String text, final String language)
    {
        final boolean       isMakefile = "makefile".equals(language);
        final String[]      lines      = text.split("\n", -1);
        final StringBuilder out        = new StringBuilder( text.length() );
        boolean              inRawMultilineLiteral = false;

        for( int i = 0; i < lines.length; ++i ) {
            String line = lines[i];

            final boolean isRecipeLine = isMakefile && !line.isEmpty() && line.charAt(0) == '\t';

            if( !isRecipeLine ) {
                final boolean entersOrExitsRaw = touchesRawMultilineLiteral(line, language);
                if(!inRawMultilineLiteral) {
                    line = stripTrailingWhitespace(line);
                }
                if(entersOrExitsRaw) inRawMultilineLiteral = !inRawMultilineLiteral;
            }

            out.append(line);
            if( i < lines.length - 1 ) out.append('\n');
        } // for

        return out.toString();
    }

    private static String stripTrailingWhitespace(final String line)
    {
        int end = line.length();
        while( end > 0 && ( line.charAt(end - 1) == ' ' || line.charAt(end - 1) == '\t' ) ) --end;
        return end == line.length() ? line : line.substring(0, end);
    }

    /**
     * Best-effort marker for whether {@code line} contains an odd number of raw-multiline-literal
     * delimiters for the given language (JS/TS backtick template literals, Python triple-quoted
     * strings) -- toggles the "currently inside a raw literal" state the caller tracks. Simple
     * count-based heuristic (not full tokenization); a line with delimiters that are themselves
     * inside a normal string/comment is a known inexactness, accepted per this feature's scope.
     */
    private static boolean touchesRawMultilineLiteral(final String line, final String language)
    {
        if( "js".equals(language) || "ts".equals(language) ) {
            return countOccurrences(line, '`') % 2 == 1;
        }
        if( "python3".equals(language) ) {
            return ( countOccurrencesOf(line, "\"\"\"") + countOccurrencesOf(line, "'''") ) % 2 == 1;
        }
        return false;
    }

    private static int countOccurrences(final String s, final char c)
    {
        int count = 0;
        for( int i = 0; i < s.length(); ++i ) if( s.charAt(i) == c ) ++count;
        return count;
    }

    private static int countOccurrencesOf(final String s, final String needle)
    {
        int count = 0;
        int idx   = 0;
        while( ( idx = s.indexOf(needle, idx) ) >= 0 ) {
            ++count;
            idx += needle.length();
        }
        return count;
    }

    // ------------------------------------------------------------------------------------------
    // normalize-invisible-invalid-unicode
    // ------------------------------------------------------------------------------------------

    private static final class LangProfile {

        final String   lineComment;
        final String[] blockCommentStart;
        final String[] blockCommentEnd;
        final char[]   stringQuotes;
        final boolean  nativeEscapeAvailable; // false => bracketed <U+XXXX> fallback everywhere
        final String   escapeStyle;           // "c" | "python" | "json" | "css" | "xml" | "none"

        LangProfile(
            final String   lineComment,
            final String[] blockCommentStart,
            final String[] blockCommentEnd,
            final char[]   stringQuotes,
            final boolean  nativeEscapeAvailable,
            final String   escapeStyle
        )
        {
            this.lineComment           = lineComment;
            this.blockCommentStart     = blockCommentStart;
            this.blockCommentEnd       = blockCommentEnd;
            this.stringQuotes          = stringQuotes;
            this.nativeEscapeAvailable = nativeEscapeAvailable;
            this.escapeStyle           = escapeStyle;
        }

    } // class LangProfile

    private static LangProfile profileFor(final String language)
    {
        switch(language) {
            case "c":
            case "cpp":
            case "java":
            case "kotlin":
            case "js":
            case "ts":
                return new LangProfile(
                    "//", new String[] { "/*" }, new String[] { "*/" },
                    new char[] { '"', '\'', '`' }, true, "c"
                );
            case "python3":
                return new LangProfile(
                    "#", new String[0], new String[0], new char[] { '"', '\'' }, true, "python"
                );
            case "json":
                return new LangProfile("", new String[0], new String[0], new char[] { '"' }, true, "json");
            case "json5":
                return new LangProfile(
                    "//", new String[] { "/*" }, new String[] { "*/" },
                    new char[] { '"', '\'' }, true, "json"
                );
            case "css":
                return new LangProfile(
                    "", new String[] { "/*" }, new String[] { "*/" },
                    new char[] { '"', '\'' }, true, "css"
                );
            case "yaml":
                return new LangProfile("#", new String[0], new String[0], new char[] { '"' }, true, "python");
            case "toml":
                return new LangProfile("#", new String[0], new String[0], new char[] { '"' }, true, "python");
            case "xml":
            case "html5":
                return new LangProfile(
                    "", new String[] { "<!--" }, new String[] { "-->" }, new char[] { '"', '\'' }, true, "xml"
                );
            case "eini":
            case "jxmake":
            case "makefile":
            case "bash":
            case "powershell":
                return new LangProfile("#", new String[0], new String[0], new char[] { '"', '\'' }, false, "none");
            default:
                return new LangProfile("", new String[0], new String[0], new char[0], false, "none");
        } // switch
    }

    /**
     * Set (a): explicit bidi-control format characters.
     */
    private static boolean isBidiControl(final int cp)
    {
        return cp == 0x202A || cp == 0x202B || cp == 0x202C || cp == 0x202D || cp == 0x202E
                || cp == 0x2066 || cp == 0x2067 || cp == 0x2068 || cp == 0x2069;
    }

    /**
     * Set (b): zero-width characters, plus a BOM found anywhere other than the very first
     * character of the whole file (checked by the caller, not here).
     */
    private static boolean isZeroWidth(final int cp)
    {
        return cp == 0x200B || cp == 0x200C || cp == 0x200D || cp == 0xFEFF;
    }

    /**
     * Set (c): non-breaking space and other Unicode space-separator lookalikes -- deliberately
     * excludes plain ASCII space (0x20) and tab/newline, which are legitimate.
     */
    private static boolean isSpaceLookalike(final int cp)
    {
        if( cp == 0x20 || cp == '\t' || cp == '\n' || cp == '\r' ) return false;
        return cp == 0x00A0 || Character.getType(cp) == Character.SPACE_SEPARATOR;
    }

    private static boolean isFlaggable(final int cp)
    {
        return isBidiControl(cp) || isZeroWidth(cp) || isSpaceLookalike(cp);
    }

    /**
     * Renders one flagged codepoint as {@code U+XXXX}, zero-padded to a minimum of 4 hex digits
     * (natural width beyond that, per the feature's exact spec -- never a fixed 6-digit pad).
     */
    private static String codepointNotation(final int cp)
    {
        String hex = Integer.toHexString(cp).toUpperCase(java.util.Locale.ROOT);
        while( hex.length() < 4 ) hex = "0" + hex;
        return "U+" + hex;
    }

    private static String bracketedComment(final int cp)
    {
        return "<" + codepointNotation(cp) + ">";
    }

    /**
     * Native per-language escape rendering for a flagged codepoint inside a string/text literal.
     * Falls back to the bracketed comment-style notation when {@code profile.nativeEscapeAvailable}
     * is false (Bash/PowerShell/Makefile/E-INI/JxMakeFile -- no consistent native invisible-unicode
     * string escape exists in these languages, per this feature's design decision).
     */
    private static String nativeStringEscape(final LangProfile profile, final int cp)
    {
        if( !profile.nativeEscapeAvailable ) return bracketedComment(cp);

        switch(profile.escapeStyle) {
            case "c":
                // JS/TS get the ES6 code-point escape for supplementary-plane values; every other
                // curly-family language (C/C++/Java/Kotlin) and the BMP case for JS/TS use plain
                // \\uXXXX (4 hex digits).
                if( cp > 0xFFFF ) return "\\u{" + Integer.toHexString(cp) + "}";
                return String.format( "\\u%04x", Integer.valueOf(cp) );
            case "python":
                // Python3: \\uXXXX (4 hex) BMP, \\UXXXXXXXX (8 hex) supplementary. YAML/TOML reuse
                // this same shape -- both specs define \\uXXXX/\\UXXXXXXXX equivalently for their
                // double-quoted/basic string forms.
                if( cp > 0xFFFF ) return String.format( "\\U%08x", Integer.valueOf(cp) );
                return String.format( "\\u%04x", Integer.valueOf(cp) );
            case "json":
                // JSON only supports \\uXXXX (no supplementary-plane single-escape form outside a
                // surrogate pair) -- JSON5 follows the same convention.
                if( cp > 0xFFFF ) {
                    final char[] pair = Character.toChars(cp);
                    return String.format( "\\u%04x", Integer.valueOf(pair[0]) )
                            + String.format( "\\u%04x", Integer.valueOf(pair[1]) );
                }
                return String.format( "\\u%04x", Integer.valueOf(cp) );
            case "css":
                // CSS hex escape: backslash + hex digits + one trailing space (terminator).
                return "\\" + Integer.toHexString(cp) + " ";
            case "xml":
                // XML/HTML5 numeric character reference -- valid in both element text and
                // attribute values.
                return "&#x" + Integer.toHexString(cp).toUpperCase(java.util.Locale.ROOT) + ";";
            default:
                return bracketedComment(cp);
        } // switch
    }

    /**
     * Whole-file scan: walks {@code text} tracking comment/string state per {@code language}'s
     * {@link LangProfile}, rewriting flagged characters in place. Identifiers and bare code
     * structure are left completely untouched -- the scan only ever mutates characters while
     * inside a recognized comment or string span. Unpaired/invalid UTF-16 surrogates are handled
     * unconditionally (everywhere, including plain code) since they have no valid "leave alone"
     * interpretation -- rendered as {@code U+FFFD} in the same per-language form as (a)/(b)/(c).
     */
    static String normalizeInvisibleUnicode(final String text, final String language)
    {
        final LangProfile  profile = profileFor(language);
        final StringBuilder out    = new StringBuilder( text.length() );

        int     i           = 0;
        boolean inLineComment  = false;
        boolean inBlockComment = false;
        char    inStringQuote  = 0;
        boolean isFirstChar    = true;

        final int n = text.length();
        while( i < n ) {
            // Unpaired/invalid surrogate -- handled everywhere, unconditionally.
            final char c = text.charAt(i);
            if( Character.isHighSurrogate(c) ) {
                if( i + 1 >= n || !Character.isLowSurrogate( text.charAt(i + 1) ) ) {
                    out.append(
                        inLineComment || inBlockComment
                                ? bracketedComment(0xFFFD)
                                : ( inStringQuote != 0 ? nativeStringEscape(profile, 0xFFFD) : bracketedComment(0xFFFD) )
                    );
                    isFirstChar = false;
                    ++i;
                    continue;
                }
            }
            if( Character.isLowSurrogate(c) && ( i == 0 || !Character.isHighSurrogate( text.charAt(i - 1) ) ) ) {
                out.append(bracketedComment(0xFFFD));
                isFirstChar = false;
                ++i;
                continue;
            }

            final int cp     = text.codePointAt(i);
            final int cpSize = Character.charCount(cp);

            if(inLineComment) {
                if( cp == '\n' ) inLineComment = false;
                else if( isFlaggable(cp) ) {
                    out.append( bracketedComment(cp) );
                    i          += cpSize;
                    isFirstChar = false;
                    continue;
                }
                out.appendCodePoint(cp);
                i += cpSize;
                isFirstChar = false;
                continue;
            }

            if(inBlockComment) {
                if( matchesAt(text, i, profile.blockCommentEnd) != null ) {
                    final String end = matchesAt(text, i, profile.blockCommentEnd);
                    out.append(end);
                    i += end.length();
                    inBlockComment = false;
                    isFirstChar    = false;
                    continue;
                }
                if( isFlaggable(cp) ) {
                    out.append( bracketedComment(cp) );
                    i          += cpSize;
                    isFirstChar = false;
                    continue;
                }
                out.appendCodePoint(cp);
                i += cpSize;
                isFirstChar = false;
                continue;
            }

            if( inStringQuote != 0 ) {
                if( cp == '\\' && i + cpSize < n ) {
                    // Preserve an existing escape sequence untouched (don't double-escape).
                    out.appendCodePoint(cp);
                    final int nextCp = text.codePointAt( i + cpSize );
                    out.appendCodePoint(nextCp);
                    i          += cpSize + Character.charCount(nextCp);
                    isFirstChar = false;
                    continue;
                }
                if( cp == inStringQuote ) {
                    out.appendCodePoint(cp);
                    i          += cpSize;
                    inStringQuote = 0;
                    isFirstChar   = false;
                    continue;
                }
                if( isFlaggable(cp) ) {
                    out.append( nativeStringEscape(profile, cp) );
                    i          += cpSize;
                    isFirstChar = false;
                    continue;
                }
                out.appendCodePoint(cp);
                i += cpSize;
                isFirstChar = false;
                continue;
            }

            // Plain code: only a BOM-not-at-file-start is flaggable here (bidi/space lookalikes in
            // bare identifiers/code structure are explicitly out of scope for this flag).
            if( !profile.lineComment.isEmpty() && matchesLiteral(text, i, profile.lineComment) ) {
                out.append(profile.lineComment);
                i             += profile.lineComment.length();
                inLineComment  = true;
                isFirstChar    = false;
                continue;
            }
            final String blockStart = matchesAt(text, i, profile.blockCommentStart);
            if( blockStart != null ) {
                out.append(blockStart);
                i              += blockStart.length();
                inBlockComment  = true;
                isFirstChar     = false;
                continue;
            }
            if( isQuoteChar(profile, c) ) {
                out.append(c);
                inStringQuote = c;
                ++i;
                isFirstChar   = false;
                continue;
            }
            if( cp == 0xFEFF && !isFirstChar ) {
                out.append( bracketedComment(cp) );
                i += cpSize;
                continue;
            }

            out.appendCodePoint(cp);
            i          += cpSize;
            isFirstChar = false;
        } // while

        return out.toString();
    }

    private static boolean isQuoteChar(final LangProfile profile, final char c)
    {
        for(final char q : profile.stringQuotes) if( q == c ) return true;
        return false;
    }

    private static boolean matchesLiteral(final String text, final int i, final String literal)
    {
        return !literal.isEmpty() && text.regionMatches( i, literal, 0, literal.length() );
    }

    private static String matchesAt(final String text, final int i, final String[] candidates)
    {
        for(final String c : candidates) if( matchesLiteral(text, i, c) ) return c;
        return null;
    }

} // class UnicodeAndWhitespaceNormalizer
