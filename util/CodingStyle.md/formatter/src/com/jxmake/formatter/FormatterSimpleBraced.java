/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import java.util.ArrayList;
import java.util.Collections;
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
     * Returns {@code c} repeated {@code count} times -- was independently re-implemented, byte-
     *  identical, as a private helper in {@link com.jxmake.formatter.rules.JsonSpecificRule},
     *  {@link com.jxmake.formatter.rules.CssSpecificRule}, and (pre-existing, unchanged here)
     *  {@code YamlTomlSharedRule} (2026-08-16 cleanup-pass consolidation); promoted here as the one
     *  shared copy since this class is already the cross-family static-helper home those two
     *  callers use for {@link #padKeysForColonAlignment}/comment normalization.
     */
    public static String repeatChar(final char c, final int count)
    {
        final StringBuilder sb = new StringBuilder(count);
        for(int i = 0; i < count; ++i) sb.append(c);

        return sb.toString();
    }

    /**
     * Returns {@code indentUnit} repeated {@code depth} times -- the per-depth indent string used by
     *  every "SimpleBraced"-shaped recursive-descent renderer (JSON/CSS directly, YAML/TOML via
     *  their own {@code indent(int)} wrapper) to build a line prefix. Same 2026-08-16 consolidation
     *  as {@link #repeatChar}.
     */
    public static String indent(final int depth, final String indentUnit)
    {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < depth; ++i) sb.append(indentUnit);

        return sb.toString();
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

    /**
     * {@code normalize-comment-end-period} for the SimpleBraced family: strips a sole trailing `.`
     *  from a {@code //}/{@code /* *&#47;}-delimited {@code commentText} (delimiters included, same
     *  shape {@link #capitalizeCommentStart} takes), same "only if it's the only `.` anywhere in the
     *  comment" rule the curly family and the tooling languages both use (an ellipsis `...` is left
     *  alone for free). For a {@code //} line comment the period must sit right at the end of the
     *  text; for a {@code /* *&#47;} block comment (which may span multiple lines as one token) it
     *  must sit right before the closing {@code *&#47;}. Operates on the delimiter-stripped interior
     *  only, so the delimiters themselves (which contain no `.`) never affect the dot count.
     */
    public static String stripCommentEndPeriod(final String commentText)
    {
        final boolean isLine  = commentText.startsWith("//");
        final boolean isBlock = commentText.startsWith("/*") && commentText.endsWith("*/");
        if(!isLine && !isBlock) return commentText;
        final int    tailLen = isBlock ? 2 : 0;
        final String head    = commentText.substring(0, 2);
        final String content = commentText.substring( 2, commentText.length() - tailLen );
        final String tail    = commentText.substring( commentText.length() - tailLen );

        int end = content.length();
        while( end > 0 && Character.isWhitespace( content.charAt(end - 1) ) ) --end;
        if( end == 0 || content.charAt(end - 1) != '.' ) return commentText;

        int dotCount = 0;
        for( int i = 0; i < content.length(); ++i ) if( content.charAt(i) == '.' ) ++dotCount;
        if(dotCount != 1) return commentText;

        int trimEnd = end - 1;
        while( trimEnd > 0 && Character.isWhitespace( content.charAt(trimEnd - 1) ) ) --trimEnd;

        return head + content.substring(0, trimEnd) + content.substring(end) + tail;
    }

    /**
     * 2026-08-08 session: give the SimpleBraced family (JSON/JSON5/CSS) curly's information
     *  architecture for {@code normalize-comment-start-case}/{@code normalize-comment-end-period} --
     *  chain consecutive standalone `//` line comments (JSON5 only; JSON/CSS never lex `//`) with no
     *  blank line between into one sentence-detection unit, and treat a multi-line `/* *&#47;` block
     *  comment already in the conventional ` * `-per-line continuation-marker banner shape as one
     *  unit too (analogous to {@code MiscRuleCore.reformatMultiLineBlockComment} and
     *  {@code XmlSpecificRule.tryBannerShape}, adapted to this family's own delimiters -- no
     *  classifier/keyword-exclusion gate, since JSON/CSS have no language keywords a comment could
     *  start with that would need protecting). {@code normalizeComment} handles a single/standalone
     *  comment (a trailing or mid-token comment, always a chain of one); {@code normalizeCommentTrivia}
     *  handles a whole run of leading comments collected between two significant tokens, grouping
     *  consecutive `//` comments and normalizing each `/* *&#47;` block comment on its own.
     */
    public static String normalizeComment(
        final String  raw,
        final boolean startCase,
        final boolean endPeriod
    )
    {
        if( raw.startsWith("//") ) return normalizeLineCommentChain(
            Collections.singletonList(raw), startCase, endPeriod
        ).get(0);

        return normalizeBlockComment(raw, startCase, endPeriod);
    }

    /**
     * Groups {@code rawTexts} (raw, un-normalized comment tokens collected in source order) into
     *  `//`-chains (consecutive, {@code blankBefore.get(k)==false}) and standalone `/* *&#47;` block
     *  comments, normalizes each group/comment, and appends the results to {@code out} in order.
     *  {@code blankBefore.get(k)} is true iff a blank line (2+ consecutive newlines) separated
     *  comment {@code k} from comment {@code k-1}; index 0's value is irrelevant (a group always
     *  starts fresh at the beginning of a sub-run).
     */
    public static void normalizeCommentTrivia(
        final List<String>  rawTexts,
        final List<Boolean> blankBefore,
        final boolean       startCase,
        final boolean       endPeriod,
        final List<String>  out
    )
    {
        final int n = rawTexts.size();
              int i = 0;
        while(i < n) {
            final String t = rawTexts.get(i);
            if( !t.startsWith("//") ) {
                out.add( normalizeBlockComment(t, startCase, endPeriod) );
                ++i;
                continue;
            }
            int j = i;
            while( j + 1 < n && rawTexts.get(
                j + 1
            ).startsWith(
                "//"
            ) && !blankBefore.get(
                j + 1
            ) ) ++j;
            out.addAll(
                normalizeLineCommentChain( rawTexts.subList(i, j + 1), startCase, endPeriod )
            );
            i = j + 1;
        } // while
    }

    /**
     * Normalizes one chain of consecutive standalone `//` comments as a single sentence-detection
     *  unit: only the first comment's start is capitalized, and the trailing `.` is stripped only if
     *  it's the sole `.` across the whole chain's content (and only from the chain's last comment).
     *  A singleton list (chain of one) is the same as the old per-comment-token behavior.
     */
    public static List<String> normalizeLineCommentChain(
        final List<String> rawTexts,
        final boolean      startCase,
        final boolean      endPeriod
    )
    {
        final List<String> out = new ArrayList<>(rawTexts);
        if(endPeriod) {
            int dotCount = 0;
            for(final String c : rawTexts) {
                final String content = c.substring(2);
                for( int i = 0; i < content.length(); ++i ) if( content.charAt(
                    i
                ) == '.' ) ++dotCount;
            } // for
            if(dotCount == 1) {
                final int    lastIdx = out.size() - 1;
                final String last    = out.get(lastIdx);
                final String content = last.substring(2);
                      int    end     = content.length();
                while( end > 0 && Character.isWhitespace( content.charAt(end - 1) ) ) --end;
                if( end > 0 && content.charAt(end - 1) == '.' ) {
                    int trimEnd = end - 1;
                    while( trimEnd > 0 && Character.isWhitespace(
                        content.charAt(trimEnd - 1)
                    ) ) --trimEnd;
                    out.set(
                        lastIdx, "//" + content.substring(0, trimEnd) + content.substring(end)
                    );
                } // if
            } // if
        } // if
        if( startCase && !out.isEmpty() ) out.set( 0, capitalizeCommentStart( out.get(0) ) );

        return out;
    }

    /**
     * Normalizes one standalone `/* *&#47;` block comment: a single-line comment gets the existing
     *  per-token treatment; a multi-line comment already in the conventional ` * `-per-line banner
     *  shape gets the same single-unit treatment via {@link #tryBannerShape}; any other multi-line
     *  shape (wrapped prose, commented-out code) is left unchanged, same posture as curly/XML-HTML5
     */
    public static String normalizeBlockComment(
        final String  raw,
        final boolean startCase,
        final boolean endPeriod
    )
    {
        if( raw.indexOf('\n') >= 0 ) {
            final String banner = tryBannerShape(raw, startCase, endPeriod);
            if(banner != null) return banner;
            // Not in the ` * `-per-line banner shape (e.g. plain wrapped prose whose content starts
            // right after `/*` on the opening line) -- fall back to the original whole-comment
            // scan: capitalize the opening line's first letter, strip a sole trailing `.` across
            // the whole multi-line content, same as this family did before banner-shape support.
        } // if
        String t = raw;
        if(endPeriod) t = stripCommentEndPeriod(t);
        if(startCase) t = capitalizeCommentStart(t);

        return t;
    }

    /**
     * Recognizes and reformats the conventional ` * `-per-line continuation-marker banner shape --
     *  every line after the first, whitespace-stripped, must start with `*`. Returns {@code null} if
     *  {@code raw} isn't in that shape. Output uses a bare {@code " *"} continuation prefix on each
     *  line (no indent baked in) -- the caller's later {@link #reindentBlockComment} pass supplies the
     *  real structural indent, matching how a freshly-collected leading comment is rendered.
     */
    private static String tryBannerShape(
        final String  raw,
        final boolean startCase,
        final boolean endPeriod
    )
    {
        final String[] rawLines = raw.split("\r\n|\r|\n", -1);
        final int      n        = rawLines.length;
        if(n < 2) return null;
        for(int i = 1; i < n; ++i) if( !stripLeadingWs( rawLines[i] ).startsWith("*") ) return null;

        int openMarkerEnd = 2;
        while( openMarkerEnd < rawLines[0].length() && rawLines[0].charAt(
            openMarkerEnd
        ) == '*' ) ++openMarkerEnd;
        final String openMarker   = rawLines[0].substring(0, openMarkerEnd);
        final String firstContent = rawLines[0].substring(openMarkerEnd).trim();

        final String lastStripped = stripLeadingWs( rawLines[n - 1] );
        final String lastContent;
        if( "*/".equals(lastStripped) ) {
            lastContent = "";
        }
        else {
            final String afterMarker = afterLeadingStarMarker(lastStripped);
            if( !afterMarker.endsWith("*/") ) return null;
            lastContent = trimTrailingWs( afterMarker.substring( 0, afterMarker.length() - 2 ) );
        }

        final List<String> contentLines = new ArrayList<>();
        if( !firstContent.isEmpty() ) contentLines.add(firstContent);
        for(int i = 1; i < n - 1; ++i) contentLines.add(
            trimTrailingWs( afterLeadingStarMarker( stripLeadingWs( rawLines[i] ) ) )
        );
        if( !lastContent.isEmpty() ) contentLines.add(lastContent);

        if( startCase && !contentLines.isEmpty() ) contentLines.set(
            0, capitalizeCommentStart( "//" + contentLines.get(0) ).substring(2)
        );
        if(endPeriod) {
            int dotCount = 0;
            for(final String l : contentLines) for(
                int i = 0; i < l.length(); ++i
            ) if( l.charAt(i) == '.' ) ++dotCount;
            if(dotCount == 1) {
                final int    lastIdx = contentLines.size() - 1;
                final String last    = contentLines.get(lastIdx);
                if( !last.isEmpty() && last.charAt( last.length() - 1 ) == '.' ) contentLines.set(
                    lastIdx, trimTrailingWs( last.substring( 0, last.length() - 1 ) )
                );
            } // if
        } // if

        final StringBuilder out = new StringBuilder(openMarker);
        for(final String line : contentLines) {
            out.append('\n').append(" *");
            if( !line.isEmpty() ) out.append(' ').append(line);
        } // for
        out.append('\n').append(" */");

        return out.toString();
    }

    private static String stripLeadingWs(final String line)
    {
        int i = 0;
        while( i < line.length() && Character.isWhitespace( line.charAt(i) ) ) ++i;

        return line.substring(i);
    }

    private static String afterLeadingStarMarker(final String wsStrippedLine)
    {
        String rest = wsStrippedLine.substring(1);
        if( rest.startsWith(" ") ) rest = rest.substring(1);

        return rest;
    }

    private static String trimTrailingWs(final String s)
    {
        int end = s.length();
        while( end > 0 && Character.isWhitespace( s.charAt(end - 1) ) ) --end;

        return s.substring(0, end);
    }

} // class FormatterSimpleBraced
