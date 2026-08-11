package com.jxmake.formatter.gdr;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal tokenizer for the GDR (general scope-depth reindentation)
 * pre-pass -- see {@code STATE_CURLY_GDR.md}. Deliberately independent of
 * {@code TokenizerCore}/{@code TokenizerCurly}: this pre-pass runs before
 * the existing formatter pipeline and must not share code with it (see
 * that state file's "Proposed pre-pass architecture" section for why).
 *
 * <p>Scoped only to what the reindenter needs: brace/paren/bracket
 * recognition (single-char tokens; depth counting itself is a later,
 * separate pre-pass stage) and exclusion-zone recognition for content
 * that must never be reindented -- string/char literals (including C++
 * raw strings and Kotlin triple-quoted strings), line/block comments,
 * and preprocessor directives (with backslash-newline continuation).
 * Everything else is emitted as opaque {@code TEXT} runs; this class
 * does not attempt to recognize identifiers, keywords, or operators.
 */
public final class GdrTokenizer {

    private final String         source;
    private final int            n;
    private       int            i;
    private       int            line;
    private       boolean        atLineStart;
    private final StringBuilder  textBuf = new StringBuilder();
    private       int            textStartLine;
    private final List<GdrToken> tokens  = new ArrayList<>();

    private GdrTokenizer(String source)
    {
        this.source = source;
        this.n      = source.length();
    }

    public static List<GdrToken> tokenize(String source)
    {
        GdrTokenizer t = new GdrTokenizer(source);

        return t.run();
    }

    private List<GdrToken> run()
    {
        i             = 0;
        line          = 0;
        atLineStart   = true;
        textStartLine = 0;

        while(i < n) {
            char c = source.charAt(i);

            if(c == '\n') {
                flushText();
                tokens.add( new GdrToken(GdrTokenType.NEWLINE, "\n", line) );
                ++line;
                ++i;
                atLineStart = true;
                continue;
            } // if

            if(c == ' ' || c == '\t' || c == '\r') {
                appendText(c);
                ++i;
                continue;
            }

            boolean isFirstOnLine = atLineStart;
            atLineStart = false;

            if(c == '#' && isFirstOnLine) {
                scanPreprocessorDirective();
                continue;
            }
            if( c == '/' && i + 1 < n && source.charAt(i + 1) == '/' ) {
                scanLineComment();
                continue;
            }
            if( c == '/' && i + 1 < n && source.charAt(i + 1) == '*' ) {
                scanBlockComment();
                continue;
            }
            if( c == '/' && scanRegexLiteralIfPresent() ) continue;
            if( c == '"' && i + 2 < n && source.charAt(
                i + 1
            ) == '"' && source.charAt(
                i + 2
            ) == '"' ) {
                scanTripleQuotedString();
                continue;
            }
            if( (c == 'R' || c == 'r') && i + 1 < n && source.charAt(
                i + 1
            ) == '"' && scanRawStringIfPresent() ) continue;
            if(c == '"') {
                scanQuoted('"', GdrTokenType.STRING);
                continue;
            }
            if(c == '\'') {
                scanQuoted('\'', GdrTokenType.CHAR);
                continue;
            }
            if(c == '`') {
                scanTemplateLiteral();
                continue;
            }

            GdrTokenType bracket = bracketTypeFor(c);
            if(bracket != null) {
                flushText();
                tokens.add( new GdrToken( bracket, String.valueOf(c), line ) );
                ++i;
                continue;
            }

            appendText(c);
            ++i;
        } // while
        flushText();

        return tokens;
    }

    private static GdrTokenType bracketTypeFor(char c)
    {
        switch(c) {
            case '{' : return GdrTokenType.BRACE_OPEN   ;
            case '}' : return GdrTokenType.BRACE_CLOSE  ;
            case '(' : return GdrTokenType.PAREN_OPEN   ;
            case ')' : return GdrTokenType.PAREN_CLOSE  ;
            case '[' : return GdrTokenType.BRACKET_OPEN ;
            case ']' : return GdrTokenType.BRACKET_CLOSE;
            default  : return null                      ;
        } // switch
    }

    private void appendText(char c)
    {
        if( textBuf.length() == 0 ) textStartLine = line;
        textBuf.append(c);
    }

    private void flushText()
    {
        if( textBuf.length() > 0 ) {
            tokens.add( new GdrToken( GdrTokenType.TEXT, textBuf.toString(), textStartLine ) );
            textBuf.setLength(0);
        }
    }

    /** Backslash-newline continues a preprocessor directive onto the next line */
    private void scanPreprocessorDirective()
    {
        flushText();
        int start     = i;
        int startLine = line;
        while(i < n) {
            char cc = source.charAt(i);
            if(cc == '\n') {
                int back = i - 1;
                if( back >= start && source.charAt(back) == '\r' ) back--;
                if( back >= start && source.charAt(back) == '\\' ) {
                    ++line;
                    ++i;
                    continue;
                }
                break;
            } // if
            ++i;
        } // while
        tokens.add(
            new GdrToken( GdrTokenType.PREPROCESSOR, source.substring(start, i), startLine )
        );
    }

    private void scanLineComment()
    {
        flushText();
        int start     = i;
        int startLine = line;
        while( i < n && source.charAt(i) != '\n' ) i++;
        tokens.add(
            new GdrToken( GdrTokenType.LINE_COMMENT, source.substring(start, i), startLine )
        );
    }

    private void scanBlockComment()
    {
        flushText();
        int start     = i;
        int startLine = line;
        i += 2;
        while( i < n && !( source.charAt(i) == '*' && i + 1 < n && source.charAt(i + 1) == '/' ) ) {
            if( source.charAt(i) == '\n' ) line++;
            ++i;
        }
        if(i < n) i += 2;
        else      i = n;
        tokens.add(
            new GdrToken( GdrTokenType.BLOCK_COMMENT, source.substring(start, i), startLine )
        );
    }

    /** Kotlin-style {@code """..."""} raw string. */
    private void scanTripleQuotedString()
    {
        flushText();
        int start     = i;
        int startLine = line;
        i += 3;
        while( i < n && !( source.charAt(i) == '"' && i + 2 < n
                && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"' ) ) {
            if( source.charAt(i) == '\n' ) line++;
            ++i;
        }
        if(i < n) i += 3;
        else      i = n;
        tokens.add( new GdrToken( GdrTokenType.STRING, source.substring(start, i), startLine ) );
    }

    /**
     * C++11-style {@code R"delim(...)delim"} raw string. Returns false (no
     * input consumed) if this isn't actually a raw-string opener -- e.g. a
     * bare identifier starting with {@code R} followed by an unrelated
     * {@code "} -- so the caller can fall through to ordinary text/string
     * handling instead.
     */
    private boolean scanRawStringIfPresent()
    {
        int           j     = i + 2;
        StringBuilder delim = new StringBuilder();
        while( j < n && source.charAt(
            j
        ) != '(' && source.charAt(
            j
        ) != '\n' && delim.length() <= 16 ) {
            delim.append( source.charAt(j) );
            ++j;
        }
        if( j >= n || source.charAt(j) != '(' ) return false;
        flushText();
        int    start     = i;
        int    startLine = line;
        String closer    = ")" + delim + "\"";
        int    idx       = source.indexOf(closer, j + 1);
        int    end       = (idx < 0) ? n : idx + closer.length();
        for(int k = start; k < end; ++k) {
            if( source.charAt(k) == '\n' ) line++;
        }
        i = end;
        tokens.add( new GdrToken( GdrTokenType.STRING, source.substring(start, end), startLine ) );

        return true;
    }

    /** Ordinary {@code "..."}/{@code '...'} literal; unterminated at a bare newline. */
    /**
     * JS/TS template literal ({@code `...`}), treated as a single opaque {@code STRING} token --
     * this deliberately does not parse {@code ${...}} interpolation (any brace/paren inside an
     * interpolation is swallowed as literal text, not counted for depth). Unlike an ordinary
     * quoted string, a template literal legitimately spans multiple physical lines, so this
     * doesn't stop at a bare newline the way {@link #scanQuoted} does.
     */
    private void scanTemplateLiteral()
    {
        flushText();
        int start     = i;
        int startLine = line;
        ++i;
        while(i < n) {
            char cc = source.charAt(i);
            if(cc == '\\' && i + 1 < n) {
                i += 2;
                continue;
            }
            if(cc == '\n') {
                ++line;
                ++i;
                continue;
            }
            if(cc == '`') {
                ++i;
                break;
            }
            ++i;
        } // while
        tokens.add( new GdrToken( GdrTokenType.STRING, source.substring(start, i), startLine ) );
    }

    private void scanQuoted(char quote, GdrTokenType type)
    {
        flushText();
        int start     = i;
        int startLine = line;
        ++i;
        while(i < n) {
            char cc = source.charAt(i);
            if(cc == '\\' && i + 1 < n) {
                i += 2;
                continue;
            }
            if(cc == '\n') break;
            if(cc == quote) {
                ++i;
                break;
            }
            ++i;
        } // while
        tokens.add( new GdrToken( type, source.substring(start, i), startLine ) );
    }

    /**
     * JS/TS regex literal ({@code /pattern/flags}), treated as a single opaque {@code STRING} token --
     * disambiguated from division by checking whether the preceding non-whitespace token permits an
     * expression start
     */
    private boolean scanRegexLiteralIfPresent()
    {
        if( !isRegexLiteralAllowedHere() ) return false;

        int     start       = i;
        int     startLine   = line;
        int     j           = i + 1; // Consume opening '/'
        boolean inCharClass = false;
        boolean closed      = false;

        while(j < n) {
            char cc = source.charAt(j);
            if(cc == '\\' && j + 1 < n) {
                j += 2;
                continue;
            }
            if(cc == '\n' || cc == '\r') break;
            if(cc == '[') {
                inCharClass = true;
                ++j;
                continue;
            }
            if(cc == ']') {
                inCharClass = false;
                ++j;
                continue;
            }
            if(cc == '/' && !inCharClass) {
                ++j;
                while( j < n && Character.isLetter( source.charAt(j) ) ) j++;
                closed = true;
                break;
            }
            ++j;
        } // while

        if(!closed) return false;

        flushText();
        tokens.add( new GdrToken( GdrTokenType.STRING, source.substring(start, j), startLine ) );
        i = j;

        return true;
    }

    private boolean isRegexLiteralAllowedHere()
    {
        int k = i - 1;
        while(k >= 0) {
            char ch = source.charAt(k);
            if(ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                --k;
                continue;
            }
            if( ch == '/' && k > 0 && source.charAt(k - 1) == '*' ) {
                k -= 2;
                while( k >= 1 && !( source.charAt(k) == '/' && source.charAt(k - 1) == '*' ) ) --k;
                if(k >= 1) k -= 2;
                else       k = -1;
                continue;
            } // if
            break;
        } // while

        if(k < 0) return true;

        char ch = source.charAt(k);

        if(ch == ')' || ch == ']' || ch == '}' || ch == '"' || ch == '\'' || ch == '`') return false;

        if(ch >= '0' && ch <= '9') return false;

        if( isIdChar(ch) ) {
            int wStart = k;
            while( wStart > 0 && isIdChar( source.charAt(wStart - 1) ) ) wStart--;
            String word = source.substring(wStart, k + 1);

            return isRegexAllowedKeyword(word);
        } // if

        if(ch == '+' || ch == '-') {
            if( k > 0 && source.charAt(k - 1) == ch ) return false; // ++ or --
        }

        return true;
    }

    private static boolean isIdChar(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '$';
    }

    private static boolean isRegexAllowedKeyword(String word)
    {
        switch(word) {
            case "return"     : /* FALL-THROUGH */
            case "yield"      : /* FALL-THROUGH */
            case "case"       : /* FALL-THROUGH */
            case "await"      : /* FALL-THROUGH */
            case "typeof"     : /* FALL-THROUGH */
            case "delete"     : /* FALL-THROUGH */
            case "void"       : /* FALL-THROUGH */
            case "do"         : /* FALL-THROUGH */
            case "else"       : /* FALL-THROUGH */
            case "in"         : /* FALL-THROUGH */
            case "instanceof" : /* FALL-THROUGH */
            case "of"         : /* FALL-THROUGH */
            case "new"        : /* FALL-THROUGH */
            case "throw"      : /* FALL-THROUGH */
            case "default"    : /* FALL-THROUGH */
            case "export"     : /* FALL-THROUGH */
            case "import"     : /* FALL-THROUGH */
            case "from"       : /* FALL-THROUGH */
            case "as"         : return true ;
            default           : return false;
        } // switch
    }

} // class GdrTokenizer
