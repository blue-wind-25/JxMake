/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

//import java.nio.charset.StandardCharsets;

import java.util.ArrayList;

import java.util.regex.Pattern;

import jxm.xb.*;


//
// Token reader class
//
public class TokenReader {

    public static class Token {
        public static enum DQPMode {
            None,
            Combine,
            Flatten
        }

        public String  path; // JxMake file path   string
        public int     lNum; // JxMake file line   number
        public int     cNum; // JxMake file column number

        public String  tStr; // Token              string
        public String  tRX1; // Regular expression string
        public String  tRX2; // Replacement        string

        public String  eStr; // Error message      string

        public DQPMode mDQP; // Preprocessing mode for a double-quoted string after evaluation
        public char    cFCS; // A flag to indicate that this token has an attached function call shortcut (zero means no attached shortcut)
        public boolean fPSP; // A flag to indicate that this token is a pure-static-string constant and will *never* need to be evaluated

        public Token(final Token token)
        {
            path = token.path;
            lNum = token.lNum;
            cNum = token.cNum;
            tStr = token.tStr;
            tRX1 = token.tRX1;
            tRX2 = token.tRX2;
            eStr = token.eStr;
            mDQP = token.mDQP;
            cFCS = token.cFCS;
            fPSP = token.fPSP;
        }

        public Token(final String path_, final int lNum_, final int cNum_, final String tStr_, final String tRX1_, final String tRX2_, final String eStr_, final boolean fPSP_)
        {

            path = path_;
            lNum = lNum_;
            cNum = cNum_;
            tStr = tStr_;
            tRX1 = tRX1_;
            tRX2 = tRX2_;
            eStr = eStr_;
            mDQP = DQPMode.None;
            cFCS = 0;
            fPSP = fPSP_;
        }

        public Token(final String path_, final int lNum_, final int cNum_, final String tStr_, final String tRX1_, final String tRX2_, final String eStr_)
        { this(path_, lNum_, cNum_, tStr_, tRX1_, tRX2_, eStr_, false); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean isError()
        { return eStr != null; }

        public boolean isEOL()
        { return !isError() && tStr.equals("\n"); }

        public boolean isSQString()
        { return !isError() && ( tStr.charAt(0) == '\'' || tStr.charAt(0) == '`' ); } // A raw-quoted string is basically a simplified single-quoted string

        public boolean isDQString()
        { return !isError() && tStr.charAt(0) == '"'; }

        public boolean isPureStaticString()
        { return !isError() && fPSP; }

        public boolean isRVarSpec()
        { return !isError() && !fPSP && tStr.length() > 3 && tStr.charAt(0) == '$'&& tStr.charAt(1) == '{'; }

        public boolean isSVarSpec()
        { return !isError() && !fPSP && tStr.length() > 3 && tStr.charAt(0) == '$'&& tStr.charAt(1) == '['; }

        public boolean hasRegExp()
        { return !tRX1.isEmpty(); }

        public boolean printError()
        { return SysUtil.printError(path, lNum, cNum, eStr); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean equals(final Token token)
        {
            return (path == token.path) &&
                   (lNum == token.lNum) &&
                   (cNum == token.cNum) &&
                   (tStr == token.tStr) &&
                   (tRX1 == token.tRX1) &&
                   (tRX2 == token.tRX2) &&
                   (mDQP == token.mDQP) &&
                   (cFCS == token.cFCS) &&
                   (fPSP == token.fPSP);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private Token(final String path_, final int lNum_, final int cNum_, final String tStr_, final String tRX1_, final String tRX2_, final DQPMode mDQP_, final char cFCS_, final boolean fPSP_)
        {
            path = path_;
            lNum = lNum_;
            cNum = cNum_;
            tStr = tStr_;
            tRX1 = tRX1_;
            tRX2 = tRX2_;
            eStr = null;
            mDQP = mDQP_;
            cFCS = cFCS_;
            fPSP = fPSP_;
        }

        public void saveToStream(final DataOutputStream dos) throws IOException
        {
            dos.writeUTF    ( path        );
            dos.writeInt    ( lNum        );
            dos.writeInt    ( cNum        );
            dos.writeUTF    ( tStr        );
            dos.writeUTF    ( tRX1        );
            dos.writeUTF    ( tRX2        );
            dos.writeUTF    ( mDQP.name() );
            dos.writeChar   ( cFCS        );
            dos.writeBoolean( fPSP        );
        }

        public static Token loadFromStream(final DataInputStream dis) throws IOException
        {
            return new Token(
                                 dis.readUTF    ()  ,
                                 dis.readInt    ()  ,
                                 dis.readInt    ()  ,
                                 dis.readUTF    ()  ,
                                 dis.readUTF    ()  ,
                                 dis.readUTF    ()  ,
                DQPMode.valueOf( dis.readUTF    () ),
                                 dis.readChar   ()  ,
                                 dis.readBoolean()
            );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String         _path         = null;
    private String         _pathHash     = null;
    private boolean        _fromJAR      = false;
    private BufferedReader _bfr          = null;
    private String         _lineStr      = null;
    private StringBuilder  _lineSB       = new StringBuilder();
    private int            _cntMLComment = 0;
    private boolean        _gotLCmt      = false;
    private boolean        _gotEval      = false;
    private boolean        _gotNewL      = false;
    private int            _cntLNum      = 0;
    private int            _curLNum      = 0;
    private int            _curCNum      = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ArrayList<TokenReader.Token> _injectedTokens      = new ArrayList<>(); // This one has a higher popping priority level
    private final ArrayList<TokenReader.Token> _injectedMacroTokens = new ArrayList<>(); // This one has a lower  popping priority level

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Token NullToken = new Token(null, 0, 0, "", "", "", null, true);

    public static Token newConstStringToken(final Token refPLCToken, final String tStr_)
    { return new Token(refPLCToken.path, refPLCToken.lNum, refPLCToken.cNum, tStr_, "", "", null, true); }

    public static Token newEmptyStringToken(final Token refPLCToken)
    { return newConstStringToken(refPLCToken, ""); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean tokMatchFirst(final ArrayList<Token> tokens, final String ref)
    { return tokens.isEmpty() ? false : tokens.get(0).tStr.equals(ref); }

    public static int tokMatchIndex(final ArrayList<Token> tokens, final String ref)
    {
        if( !tokens.isEmpty() ) {
            for(int i = 0; i < tokens.size(); ++i) {
                if( tokens.get(i).tStr.equals(ref) ) return i;
            }
        }

        return -1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Token tokMatchPopFirst(final ArrayList<Token> tokens, final Token refPLCToken, final String ref, final boolean mustNotNull)
    {
        if( !tokens.isEmpty() ) {
            if( tokens.get(0).tStr.equals(ref) ) return tokens.remove(0);
        }

        return mustNotNull ? newEmptyStringToken(refPLCToken) : null;
    }

    public static Token tokPopFirst(final ArrayList<Token> tokens, final Token refPLCToken, final boolean mustNotNull)
    {
        if( !tokens.isEmpty() ) return tokens.remove(0);

        return mustNotNull ? newEmptyStringToken(refPLCToken) : null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean strMatchPopFirst(final ArrayList<Token> tokens, final String ref)
    {
        if( !tokens.isEmpty() ) {
            if( tokens.get(0).tStr.equals(ref) ) {
                tokens.remove(0);
                return true;
            }
        }

        return false;
    }

    public static String strPopFirst(final ArrayList<Token> tokens)
    { return tokens.isEmpty() ? "" : tokens.remove(0).tStr; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _clear() throws IOException
    {
        _bfr.close();

        _path    = null;
        _bfr     = null;
        _lineStr = null;
        _gotLCmt = false;
        _gotNewL = false;
        _cntLNum = 0;
        _curLNum = 0;
        _curCNum = 0;
    }

    private String _readRawLine() throws IOException
    {
        final String line = _bfr.readLine();

        if(line == null) return null;

        ++_cntLNum;

        return line;
    }

    private void _readLine() throws IOException
    {
        // Clear the line and the current column number
        _lineStr = "";
        _curCNum = 0;

        // Read and combine line(s)
        int cntCombLine = 0;

        while(true) {

            // Read one line
            final String line = _bfr.readLine(); // NOTE : Do not use 'StringBuilder' here because the chance of '\\' appearing would be quite rare
            if(line == null) break;
            ++_cntLNum;

            // Check if the line is empty
            if( line.length() == 0 ) continue;

            // Check if the last character is the line continuation character
            if( line.charAt( line.length() - 1 ) == '\\' ) {
                // Concatenate the line (minus the last character)
                _lineStr += line.substring( 0, line.length() - 1 );
                ++cntCombLine;
            }
            // The last character is not the line continuation character
            else {
                // Concatenate the line and break
                _lineStr    += line;
                _curLNum     = _cntLNum - cntCombLine;
                cntCombLine  = 0;
                break;
            }

        } // while true

        // If the line string length is zero, it means the file has reached EOF
        if( _lineStr.length() == 0 ) _clear();
    }

    private boolean _isSepOper(final char ch)
    {
        return _gotEval ? "(),"  .indexOf(ch) != -1
                        : "(),&!".indexOf(ch) != -1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public TokenReader(final String jxmSpecFile_absPath, final boolean fromJAR) throws IOException
    {
        /*
        SysUtil.stdDbg().printf("%b %s\n", fromJAR, jxmSpecFile_absPath);
        //*/

        if(fromJAR) {
            final String jarRes = jxmSpecFile_absPath.substring( SysUtil._JxMakeProgramJARResPrefix.length() ); // Remove the '_JxMakeProgramJARResPrefix' prefix
            _bfr = new BufferedReader( new InputStreamReader( JxMake.class.getResourceAsStream(jarRes), SysUtil._CharEncoding ) );
        }
        else {
            _bfr = new BufferedReader( new InputStreamReader( new FileInputStream(jxmSpecFile_absPath), SysUtil._CharEncoding ) );
        }

        _path     = jxmSpecFile_absPath;
        _pathHash = SysUtil.computeCRC32  (_path);
      //_pathHash = SysUtil.computeMD2Hash(_path);
        _fromJAR  = fromJAR;

        /*
        SysUtil.stdDbg().println("### " + _pathHash + " " + _path);
        //*/
    }

    public String inputJXMSpecFile_absPath()
    { return _path; }

    public boolean isFromJAR()
    { return _fromJAR; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void injectTokens(final ArrayList<TokenReader.Token> tokens)
    { for(final Token token : tokens) _injectedTokens.add( new Token(token) ); }

    public void injectTokens(final TokenReader.Token... tokens)
    { for(final Token token : tokens) _injectedTokens.add( new Token(token) ); }

    public void injectTokens(final ArrayList<TokenReader.Token> tokens, final TokenReader.Token... extraTokens)
    {
        for(final Token token : tokens     ) _injectedTokens.add( new Token(token) );
        for(final Token token : extraTokens) _injectedTokens.add( new Token(token) );
    }

    public void injectToken_EOL(final TokenReader.Token refPLCToken)
    { _injectedTokens.add( newConstStringToken(refPLCToken, "\n") ); }

    public void injectToken_Colon(final TokenReader.Token refPLCToken)
    { _injectedTokens.add( newConstStringToken(refPLCToken, ":") ); }

    public void injectToken_endif(final TokenReader.Token refPLCToken)
    { _injectedTokens.add( newConstStringToken(refPLCToken, "endif") ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void injectMacroTokens(final ArrayList<TokenReader.Token> tokens)
    { for(final Token token : tokens) _injectedMacroTokens.add( new Token(token) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getSpecFilePathHash()
    { return _pathHash; }

    public Token readToken() throws IOException
    {
        // Read from the injected tokens first according to their containers' popping priority level
        if( !_injectedTokens     .isEmpty() ) return _injectedTokens     .remove(0);
        if( !_injectedMacroTokens.isEmpty() ) return _injectedMacroTokens.remove(0);

        // Loop until a token is acquired or EOF
        while(true) {

            // Read one line as needed
            if(_lineStr == null) {
                _readLine();
                if(_lineStr == null) break;
            }

            // State variables
            String        tStr     = ""; // NOTE : Do not use 'StringBuilder' here to make string comparison and manipulation easier
            String        tRX1     = ""; // NOTE : Do not use 'StringBuilder' here because regular expressions should not appear that often
            String        tRX2     = ""; // NOTE : Do not use 'StringBuilder' here because regular expressions should not appear that often
            String        eStr     = null;
            char          lch      = 0;
            boolean       inRQStr  = false;
            boolean       inSQStr  = false;
            boolean       inDQStr  = false;
            boolean       inVarEvl = false;
            int           inRegExp = 0;
            Token.DQPMode dqpMode  = Token.DQPMode.None;
            char          fcsChar  = 0;
            int           tokPos   = 0;
            boolean       endSDQ   = false;

            // Walk through the characters
            if(!_gotLCmt) {

                // Shortcuts
                final int lineStrLen = _lineStr.length();
                final int startCol   = _curCNum;

                // Walk through the characters
                boolean gotStr = false;
                boolean gotVar = false;
                boolean nSepOp = false;

                for(int i = startCol; i < lineStrLen; ++i) {

                    // Check if currently inside a multiline comment
                    final boolean insideMLComment = (_cntMLComment != 0);

                    // Read one character
                    final char ch = _lineStr.charAt(i);
                    ++_curCNum;

                    // Save the token position as needed
                    if(tokPos == 0) tokPos = _curCNum;

                    // Check if it is currently inside `` or '' or ""
                    if(inRQStr || inSQStr || inDQStr) {
                        // Check for the escape character
                        if(!inRQStr && ch == '\\') {
                            final char nch = (i + 1 < lineStrLen) ? _lineStr.charAt(++i) : '\\';
                            ++_curCNum;
                            if(inSQStr) {
                                // In a single-quoted string, only '\'' and '\\' are evaluated
                                if( nch != '\'' && nch != '\\' ) tStr += '\\';
                                                                 tStr += nch;
                                endSDQ = (nch == '\'') && (i + 1 == lineStrLen);
                            }
                            else {
                                // In a double-quoted string, simply store the escape sequence so it can be processed later
                                tStr += '\\';
                                tStr += nch;
                                endSDQ = (nch == '"') && (i + 1 == lineStrLen);
                            }
                        }
                        // Not an escape character
                        else {
                            tStr += ch;
                                 if(inRQStr && ch == '`' ) { inRQStr = false; gotStr = true; }
                            else if(inSQStr && ch == '\'') { inSQStr = false; gotStr = true; }
                            else if(inDQStr && ch == '"' ) { inDQStr = false; gotStr = true; }
                        }
                    }

                    // Check if it is currently inside ///
                    else if(inRegExp != 0) {
                        // Check for the escape character
                        if(ch == '\\') {
                            final char nch = (i + 1 < lineStrLen) ? _lineStr.charAt(++i) : '\\';
                            ++_curCNum;
                            if(inRegExp == 1) {
                                tRX1 += '\\';
                                tRX1 += nch;
                            }
                            else {
                                tRX2 += '\\';
                                tRX2 += nch;
                            }
                        }
                        // Not an escape character
                        else {
                            if(ch == '/') {
                                ++inRegExp;
                                if(inRegExp >= 3) inRegExp = 0;
                            }
                            else {
                                if(inRegExp == 1) tRX1 += ch;
                                else              tRX2 += ch;
                            }
                        }
                    }

                    // Outside '' and "" and ///
                    else {
                        // Skip whitespace
                        if( Character.isWhitespace(ch) ) {
                            if( tStr.length() != 0 ) {
                                // Do not break if currently inside ${...} or $[...]
                                if(!inVarEvl) break;
                            }
                            tokPos = 0;
                            continue;
                        }
                        // Check for the opening character sequence of a multiline comment that are not preceded by whitespace
                        if( lch == '*' && tStr.endsWith("(*") ) {
                            tStr = tStr.substring( 0, tStr.length() - 2 );
                            _curCNum -= 3;
                            break;
                        }
                        // Check for multiline double-quoted string
                        if( ch == '[' && _lineStr.indexOf("[[\"") >= 0 ) {
                            final String chkStr = _lineStr.substring( i, _lineStr.length() ).trim();
                            if( !chkStr.trim().equals("[[\"" ) ) {
                                eStr = Texts.EMsg_InvalidDQStrMLBeg;
                                break;
                            }
                            else {
                                // Check for .[["..."]] or :[["..."]]
                                if( _lineStr.indexOf(".[[\"") >= 0 ) dqpMode = Token.DQPMode.Combine;
                                if( _lineStr.indexOf(":[[\"") >= 0 ) dqpMode = Token.DQPMode.Flatten;
                                // Gather the multiline double-quoted string lines
                                _lineSB.setLength(0);
                                while(true) {
                                    // Read a line
                                    final String line = _readRawLine();
                                    if(line == null) {
                                        eStr = Texts.EMsg_PrematureEOF;
                                        break;
                                    }
                                    // Check for the closing token
                                    if( line.indexOf("\"]]") >= 0 && line.indexOf("\\\"]]") < 0 ) {
                                        if( !line.trim().equals("\"]]") ) eStr = Texts.EMsg_InvalidDQStrMLEnd;
                                        break;
                                    }
                                    // Not a closing token
                                    else {
                                        if( _lineSB.length() > 0 ) _lineSB.append("\n");
                                                                   _lineSB.append(line);
                                    }
                                }
                                // Store the multiline double-quoted string
                                                                  _lineSB.insert  (0, '"');
                                                                  _lineSB.append  (   '"');
                                if( _lineSB.length() > 0 ) tStr = _lineSB.toString(      );
                                _lineStr = "";
                                break;
                            }
                        }
                        // Break if it is a single-line comment character prefix (only if it is not currently inside a multiline comment)
                        else if(ch == '#' && !insideMLComment) {
                            if(_curCNum == 1) _lineStr = null; // Reset if the line only contains comments
                            else              _gotLCmt = true; // Otherwise, set the flag
                            break;
                        }
                        // Break if it is a separated operator
                        else if( _isSepOper(ch) ) {
                            // Get the next character
                            final char nch = ( (i + 1) < lineStrLen ) ? _lineStr.charAt(i + 1) : 0;
                            // Do not separate '&==', '&!=', '!=', '(*', and '*)'
                            if( (ch == '&' && nch == '=') || (ch == '&' && nch == '!') || (ch == '!' && nch == '=') ||
                                (ch == '(' && nch == '*') || (ch == ')' && lch == '*')
                              ) {
                                nSepOp = true;
                            }
                            // Check if the token is not empty
                            else if( tStr.length() != 0 ) {
                                --_curCNum; // Backtrack one character
                                break;
                            }
                        }
                        else if(!insideMLComment) {
                            // Check for ';'
                            if(ch == ';') {
                                // Create a new line token
                                final Token nlToken = new Token(_path, _curLNum, tokPos, "\n", "", "", null);
                                // Return the new line token if there is no pending token
                                if( tStr.isEmpty() ) return nlToken;
                                // Otherwise, store the new line token and break
                                _injectedTokens.add( new Token(_path, _curLNum, tokPos, "\n", "", "", null) );
                                break;
                            }
                            // Check for '\' which is followed by ' '
                            else if(ch == '\\') {
                                // Error if there is no more character
                                final char nch = (i + 1 < lineStrLen) ? _lineStr.charAt(++i) : 0;
                                ++_curCNum;
                                if(nch == 0) {
                                    eStr = XCom.errorString(Texts.EMsg_PrematureEOL);
                                    break;
                                }
                                // Append the character if the next character is not ' '
                                if(nch != ' ') tStr += ch;
                                // Append the next character
                                tStr += nch;
                                continue;
                            }
                            // Check for the beginning of ."..."
                            else if(ch == '.') {
                                if( (i + 1) < lineStrLen ) {
                                    final char nch = _lineStr.charAt(i + 1);
                                    if(nch == '"') {
                                        dqpMode = Token.DQPMode.Combine;
                                        continue;
                                    }
                                    else if(nch == '\'') {
                                        eStr = XCom.errorString(Texts.EMsg_IllegalCFOperSQStr);
                                        break;
                                    }
                                }
                            }
                            // Check for the beginning of :"..."
                            else if(ch == ':') {
                                if( (i + 1) < lineStrLen ) {
                                    final char nch = _lineStr.charAt(i + 1);
                                    if(nch == '"') {
                                        dqpMode = Token.DQPMode.Flatten;
                                        continue;
                                    }
                                    else if(nch == '\'') {
                                        eStr = XCom.errorString(Texts.EMsg_IllegalCFOperSQStr);
                                        break;
                                    }
                                }
                            }
                            // Check for the beginning of ~${... ~$[...
                            else if(!_gotEval && ch == '~') {
                                if( (i + 1) < lineStrLen && _lineStr.charAt(i + 1) == '$' ) {
                                    fcsChar = '~';
                                    continue;
                                }
                            }
                            // Check for the beginning of ^${... ^$[...
                            else if(ch == '^') {
                                if( (i + 1) < lineStrLen && _lineStr.charAt(i + 1) == '$' ) {
                                    fcsChar = '^';
                                    continue;
                                }
                            }
                            // Check for the beginning of -${... -$[...
                            else if(!_gotEval && ch == '-') {
                                if( (i + 1) < lineStrLen && _lineStr.charAt(i + 1) == '$' ) {
                                    // Ensure -$func(... is excluded
                                    if( (i + 2) < lineStrLen ) {
                                        final char nch = _lineStr.charAt(i + 2);
                                        if(nch == '{' || nch == '[') {
                                            fcsChar = '-';
                                            continue;
                                        }
                                    }
                                }
                            }
                            // Other characters - check for special characters
                                 if(                  ch == '`'                ) { inRQStr  = true ;                }
                            else if(                  ch == '\''               ) { inSQStr  = true ;                }
                            else if(                  ch == '"'                ) { inDQStr  = true ;                }
                            else if(  lch == '$' && ( ch == '{' || ch == '[' ) ) { inVarEvl = true ;                }
                            else if(  inVarEvl   && ( ch == '}' || ch == ']' ) ) { inVarEvl = false; gotVar = true; }
                            else if(  inVarEvl   &&   ch == '/'                ) { inRegExp = 1    ;                }
                            else if( !inVarEvl   &&   ch == '^'                ) { tStr += '^'; continue;           }
                            // Check there are character(s) in the token before ${ or $[ outside of 'eval' statement
                            if( !_gotEval && inVarEvl && tStr.charAt(0) != '$' ) {
                                // Backtrack two character
                                _curCNum -= 2;
                                // Remove the '$' from the end of the token
                                tStr = tStr.substring( 0, tStr.length() - 1 );
                                // Reset flag
                                inVarEvl = false;
                                break;
                            }
                        }
                        // Append the character
                        if(inRegExp == 0) tStr += ch;
                        // Break if it is one of the special tokens
                        if(nSepOp) {
                            final int idx = tStr.indexOf("*)"); // Check for the closing character sequence of a multiline comment
                            if(idx != -1) {
                                final int len = tStr.length();
                                if(len == 2) break;
                                if(idx == len - 2) {
                                    // Remove the '*)' from the token
                                    tStr = tStr.substring(0, len - 2);
                                    // Inject the '*)' token and update the multiline comment counter
                                    if( !tStr.isEmpty() ) {
                                        _injectedTokens.add( new Token(_path, _curLNum, tokPos, "*)", "", "", null) );
                                        --_cntMLComment;
                                    }
                                    break;
                                }
                            }
                        }
                        else if(!nSepOp) {
                            // Break if it is a separated operator
                            if( tStr.length() == 1 && _isSepOper( tStr.charAt(0) ) ) break;
                            // Break if it is the opening character sequence of a multiline comment
                            if( tStr.equals("(*") ) break;
                            // Break if it is one of the '@' variants
                            if(!insideMLComment && !_gotEval) {
                                if( tStr.equals(  "@") || tStr.equals( "?@") ||
                                    tStr.equals( "-@") || tStr.equals( "+@") ||
                                    tStr.equals("-+@") || tStr.equals("+-@")
                                ) {
                                     if( tStr.equals( "+-@") ) tStr = "-+@";
                                     break;
                                }
                            }
                        }
                        // Reset flag
                        nSepOp = false;
                    }

                    // Skip the rest if currently inside a multiline comment
                    if(insideMLComment) {
                        lch = ch;
                        continue;
                    }

                    // Check the regular variable name
                    if( gotVar && tStr.charAt(1) == '{' ) {
                        // Check if it is a valid symbol name
                        if( !XCom.isSymbolName( XCom.trmRVarName(tStr), true ) ) eStr = XCom.errorString( Texts.EMsg_InvalidVarName, XCom.trmRVarName(tStr) );
                    }

                    // Convert the shortcuts : '${...}<N/V>' to '$partn[m](${...}, N)' or '$partn[m](${...}, ${V})'
                    //                         '$[...]<N/V>' to '$partn[m]($[...], N)' or '$partn[m]($[...], ${V})'
                    if( gotVar && (i + 2) < lineStrLen &&  _lineStr.charAt(i + 1) == '<' ) {
                        // Get the index
                        int     newCNum = _curCNum;
                        String  idxStr  = "";
                        boolean sValid  = false;
                        for(int x = i + 2; x < lineStrLen; ++x) {
                            final char xch = _lineStr.charAt(x);
                            ++newCNum;
                            if(xch == '>') {
                                sValid = true;
                                break;
                            }
                            idxStr += xch;
                        }
                        // Check if we have got a valid shortcut expression
                        if(sValid) {
                            // Check for '$partn' or '$partnm'
                            final boolean n  = ( idxStr.indexOf(',') > 0 );
                            final boolean nm = ( idxStr.indexOf(':') > 0 );
                            // Error if the index string is empty or both ',' and ':' exists
                            if( idxStr.isEmpty() || (n && nm) ) {
                                eStr = XCom.errorString( Texts.EMsg_InvalidPartXIndexFor, "n[m]", tStr, "?[,|: ?]" );
                            }
                            // The index is not empty
                            else {
                                // Update the column position
                                _curCNum = newCNum + 1;
                                // Split the indexes
                                final ArrayList<String> idx = XCom.explode(idxStr, nm ? ":" : ",");
                                // Check the number of indexes
                                final int numIdx = idx.size();
                                if(numIdx > 2) {
                                    eStr = XCom.errorString( Texts.EMsg_InvalidPartXIndexFor, nm ? "nm" : "n", tStr, nm ? "?:?" : ( (numIdx > 2) ? "?, ?" : "?" ) );
                                }
                                // The number of indexes is within the limit
                                else {
                                    // Get the 1st index
                                    String idx1 = idx.get(0).trim();
                                    if( XCom.isSymbolName(idx1) ) idx1 = XCom.genRVarName(idx1);
                                    // Get the optional 2nd index
                                    String idx2 = (numIdx == 2) ? idx.get(1).trim() : "";
                                    if( XCom.isSymbolName(idx2) ) idx2 = XCom.genRVarName(idx2);
                                    // Inject tokens
                                    final String fname = nm ? "$partnm" : "$partn";
                                        injectTokens(
                                            new Token(_path, _curLNum, tokPos, fname, ""  , ""  , null),
                                            new Token(_path, _curLNum, tokPos, "("  , ""  , ""  , null),
                                            new Token(_path, _curLNum, tokPos, tStr , tRX1, tRX2, eStr),
                                            new Token(_path, _curLNum, tokPos, ","  , ""  , ""  , null),
                                            new Token(_path, _curLNum, tokPos, idx1 , ""  , ""  , null)
                                        );
                                    if(numIdx == 2) {
                                        injectTokens(
                                            new Token(_path, _curLNum, tokPos, ","  , ""  , ""  , null),
                                            new Token(_path, _curLNum, tokPos, idx2 , ""  , ""  , null)
                                        );
                                    }
                                        injectTokens(
                                            new Token(_path, _curLNum, tokPos, ")"  , ""  , ""  , null)
                                        );
                                    // Done for now
                                    return readToken();
                                }
                            }
                        } // if sValid
                    } // if gotVar ...

                    // Break if it has got a complete token
                    if(gotStr || gotVar) break;

                    // Save the last character
                    lch = ch;

                } // for i

                // Check for incomplete token
                if( inVarEvl || (inRegExp != 0) ) eStr = Texts.EMsg_PrematureEOL;

            } // if !_gotLCmt

            // Go back to top if the line has been resetted
            if(_lineStr == null) continue;

            // Check for the opening character sequence of a multiline comment that are not preceded by whitespace
            if( tStr.length() > 2 && tStr.endsWith("(*") ) {
                tStr = tStr.substring( 0, tStr.length() - 2 );
                _curCNum -= 2;
            }

            // Check form empty string
            if( tStr.length() == 0 ) {
                _lineStr = null;
                _gotLCmt = false; // Reset the flag
                tStr     = "\n";
                tokPos   = _curCNum;
            }

            // Make sure no consecutive newline characters are returned
            if( tStr.equals("\n") ) {
                if(_gotNewL) continue;
                _gotNewL = true;
                _gotEval = false;
            }
            else {
                _gotNewL = false;
            }

            // Update the multiline comment counter
            if( tStr.equals("(*") ) ++_cntMLComment;
            if( tStr.equals("*)") ) --_cntMLComment;

            // Check if currently inside a multiline comment
            final boolean insideMLComment = (_cntMLComment != 0);

            // Check for unclosed raw-quoted string
            if( !insideMLComment && tStr.charAt(0) == '`' ) {
                final boolean err = ( tStr.length() < 2 ) || ( tStr.charAt( tStr.length() - 1 ) != '`' );
                if(err) eStr = Texts.EMsg_UnclosedRQStr;
            }

            // Check for unclosed single-quoted string
            if( !insideMLComment && tStr.charAt(0) == '\'' ) {
                boolean err = ( tStr.length() < 2 ) || ( tStr.charAt( tStr.length() - 1 ) != '\'' );
                if(endSDQ) err |= ( tStr.length() < 3 ) || ( tStr.charAt( tStr.length() - 2 ) != '\'' );
                if(err) eStr = Texts.EMsg_UnclosedSQStr;
            }

            // Check for unclosed double-quoted string
            if( !insideMLComment && tStr.charAt(0) == '"'  ) {
                boolean err = ( tStr.length() < 2 ) || ( tStr.charAt( tStr.length() - 1 ) != '"' );
                if(endSDQ) err |= ( tStr.length() < 3 ) || ( tStr.charAt( tStr.length() - 2 ) != '"' );
                if(err) eStr = Texts.EMsg_UnclosedDQStr;
            }

            // Check for empty regular expression
            if( !insideMLComment && tRX1.isEmpty() && !tRX2.isEmpty() ) eStr = Texts.EMsg_InvalidRegExp;

            // Create a new token
            final Token token = new Token(_path, _curLNum, tokPos, tStr, tRX1, tRX2, eStr);

            // Check for ."..." and :"..."
            if(!insideMLComment && dqpMode != Token.DQPMode.None) {
                token.mDQP = dqpMode;
                dqpMode    = Token.DQPMode.None;
            }

            // Check for ~${... ~$[... and ^${... ^$[... and -${... -$[...
            if(!insideMLComment && fcsChar != 0) {
                token.cFCS = fcsChar;
                fcsChar    = 0;
            }

            // Check for {{ and }}
            if(!insideMLComment) {
                if( token.tStr.equals("{{") ) {
                    injectTokens( new Token(_path, _curLNum, tokPos, "(", "", "", null) );
                    token.tStr = "$stack";
                }
                else if( token.tStr.equals("}}") ) {
                    token.tStr = ")";
                }
            }

            // Check for 'eval' statement
            if( !insideMLComment && token.tStr.equals("eval") ) _gotEval = true;

            // Return the token
            return token;

        } // while true

        // No token can be acquired
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Token unquoteSQString(final Token token)
    {
        // Check if it is not a single-quoted string
        if( !token.isSQString() ) return token;

        // Unquote the string
        token.tStr = token.tStr.substring( 1, token.tStr.length() - 1 );

        // Set the flag
        token.fPSP = true;

        // Return the token
        return token;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _reTrimWSInVarName = Pattern.compile("(\\$[\\[\\{])\\s*(\\S+)\\s*([\\}\\]])");

    private static class EscSeqODSUU {
        public final String eStr;
        public final String dChr;
        public final int    dCnt;

        public EscSeqODSUU(final String eStr_, final String dChr_, final int dCnt_)
        {
            eStr = eStr_;
            dChr = dChr_; // new String( dChr_.getBytes(), StandardCharsets.UTF_8 );
            dCnt = dCnt_;
        }
    }

    private static EscSeqODSUU _parseEscSeq_odsuU(final String tokenStr, final int startIdx, final int tokenStrLen, final char chrRadix)
    {
        // Determine the radix
        final int    radix  = (chrRadix == 'o') ?  8
                            : (chrRadix == 'd') ? 10
                            : (chrRadix == 'x') ? 16
                            : (chrRadix == 'u') ? 16
                            : (chrRadix == 'U') ? 16
                            :                      0;

        // Determine the number of characters in the sequence
        final int    cntSeq = (chrRadix == 'o') ? 3
                            : (chrRadix == 'd') ? 3
                            : (chrRadix == 'x') ? 2
                            : (chrRadix == 'u') ? 4
                            : (chrRadix == 'U') ? 6
                            :                     0;

        // Gather the characters
        final char[] chrSeq = new char[cntSeq];
              int    i      = startIdx;

                        chrSeq[0] = (i + 1 < tokenStrLen) ? tokenStr.charAt(++i) : 0;
                        chrSeq[1] = (i + 1 < tokenStrLen) ? tokenStr.charAt(++i) : 0;
        if(cntSeq >= 3) chrSeq[2] = (i + 1 < tokenStrLen) ? tokenStr.charAt(++i) : 0;
        if(cntSeq >= 4) chrSeq[3] = (i + 1 < tokenStrLen) ? tokenStr.charAt(++i) : 0;
        if(cntSeq >= 6) chrSeq[4] = (i + 1 < tokenStrLen) ? tokenStr.charAt(++i) : 0;
        if(cntSeq >= 6) chrSeq[5] = (i + 1 < tokenStrLen) ? tokenStr.charAt(++i) : 0;

        // Convert the character sequence to string
        final String strSeq = new String(chrSeq);

        try {
            // Parse integer
            final int intVal = Integer.parseInt(strSeq, radix);
            // Check range
            if( cntSeq < 4 && (intVal < 0 || intVal > 255) ) throw XCom.newException("");
            // Return the result
            return new EscSeqODSUU( null, String.valueOf( Character.toChars(intVal) ), cntSeq );
          //if( cntSeq > 4) return new EscSeqODSUU( null, String.valueOf( Character.toChars(intVal) ), cntSeq );
          //else            return new EscSeqODSUU( null, String.valueOf(            (char) intVal  ), cntSeq );
        }
        catch(final Exception e) {
            // Return the error message
                 if(radix ==  8               ) return new EscSeqODSUU( XCom.errorString(Texts.EMsg_InvalidEscSeqOctValue, "\\o" + strSeq), null, cntSeq );
            else if(radix == 10               ) return new EscSeqODSUU( XCom.errorString(Texts.EMsg_InvalidEscSeqDecValue, "\\d" + strSeq), null, cntSeq );
            else if(radix == 16 && cntSeq == 2) return new EscSeqODSUU( XCom.errorString(Texts.EMsg_InvalidEscSeqHexValue, "\\x" + strSeq), null, cntSeq );
            else if(radix == 16 && cntSeq == 4) return new EscSeqODSUU( XCom.errorString(Texts.EMsg_InvalidEscSeqUniValue, "\\u" + strSeq), null, cntSeq );
            else if(radix == 16 && cntSeq == 6) return new EscSeqODSUU( XCom.errorString(Texts.EMsg_InvalidEscSeqUniValue, "\\U" + strSeq), null, cntSeq );
        }

        // Unknown error
        return new EscSeqODSUU( XCom.errorString(Texts.EMsg_UnknownCompileError), null, 0 );
    }

    public static ArrayList<Token> splitDQString(final Token token)
    {
        // Check if it is not a double-quoted string
        if( !token.isDQString() ) return null;

        // List of tokens
        ArrayList<Token> tokens = new ArrayList<>();

        // Check for empty string
        if( token.tStr.equals("\"\"") ) {
            tokens.add( new Token(token.path, token.lNum, token.cNum, "''", "", "", null, true ) );
            return tokens;
        }

        // State variables
        String  tStr     = "";
        String  tRX1     = "";
        String  tRX2     = "";
        String  eStr     = null;
        boolean inVarEvl = false;
        int     inRegExp = 0;
        char    fcsChar  = 0;

        // Walk through the characters
        final int lineStrLen = token.tStr.length();

        for(int i = 1; i < lineStrLen - 1; ++i) { // Skip the opening and closing '"'

            // Read one character
            final char ch = token.tStr.charAt(i);

            // Check if it is currently inside ///
            if(inRegExp != 0) {
                // Check for the escape character
                if(ch == '\\') {
                    final char nch = (i + 1 < lineStrLen) ? token.tStr.charAt(++i) : '\\';
                    if(inRegExp == 1) {
                        tRX1 += '\\';
                        tRX1 += nch;
                    }
                    else {
                        tRX2 += '\\';
                        tRX2 += nch;
                    }
                }
                // Not an escape character
                else {
                    if(ch == '/') {
                        ++inRegExp;
                        if(inRegExp >= 3) inRegExp = 0;
                    }
                    else {
                        if(inRegExp == 1) tRX1 += ch;
                        else              tRX2 += ch;
                    }
                }
            }

            // Outside ///
            else {
                // Check for the escape character
                if(ch == '\\') {
                    // Get the next character
                    final char nch = (i + 1 < lineStrLen) ? token.tStr.charAt(++i) : '\\';
                    // Append the character
                    // NOTE : Synchronize them with what is listed in '../../docs/txt/en_US/02-Defining-Strings.txt' (and its translations) !!!
                    switch(nch) {
                        case 't'  : tStr += '\t'    ; break;
                        case 'v'  : tStr += '\u000B'; break;
                        case 'r'  : tStr += '\r'    ; break;
                        case 'n'  : tStr += '\n'    ; break;
                        case 'f'  : tStr += '\f'    ; break;
                        case 'b'  : tStr += '\b'    ; break;
                        case '\'' : tStr += '\''    ; break;
                        case '"'  : tStr += '\"'    ; break;
                        case '\\' : tStr += '\\'    ; break;
                        case '$'  : tStr += '$'     ; break;
                        case '~'  : tStr += '~'     ; break;
                        case '`'  : tStr += '`'     ; break;
                        case '-'  : tStr += '-'     ; break;
                        case '+'  : tStr += '+'     ; break;
                        case '?'  : tStr += '?'     ; break;
                        case 'o'  : /* FALLTHROUGH */
                        case 'd'  : /* FALLTHROUGH */
                        case 'x'  : /* FALLTHROUGH */
                        case 'u'  : /* FALLTHROUGH */
                        case 'U'  : if(true) {
                                        final EscSeqODSUU res = _parseEscSeq_odsuU(token.tStr, i, lineStrLen, nch);
                                        if(res.eStr != null) eStr = res.eStr;
                                        tStr += res.dChr;
                                        i    += res.dCnt;
                                    }
                                    break;
                        default   : tStr += nch     ; break;
                    }
                    // Go back to top
                    continue;
                }
                // Check for "${" and "$["
                if(ch == '$') {
                    // Get the next character
                    final char nch = (i + 1 < lineStrLen) ? token.tStr.charAt(i + 1) : ' ';
                    // Check for '{' or '['
                    if( (nch == '{' || nch == '[') ) {
                        // Store the token as needed
                        if( tStr.length() > 0 ) {
                            /*
                            // Check for empty regular expression
                            if( tRX1.isEmpty() && !tRX2.isEmpty() ) eStr = Texts.EMsg_InvalidRegExp;
                            // Store the token
                            tokens.add( new Token(token.path, token.lNum, token.cNum, "'" + tStr + "'", tRX1, tRX2, eStr, true) );
                            */
                            // Store the token (at this point, it should be a constant)
                            if( !tRX1.isEmpty() || !tRX2.isEmpty() ) eStr = XCom.errorString(Texts.EMsg_UnexpectedToken, tStr);
                            tokens.add( new Token(token.path, token.lNum, token.cNum, "'" + tStr + "'", "", "", eStr, true ) );
                            // Clear variables
                            tStr = "";
                          //tRX1 = "";
                          //tRX2 = "";
                            eStr = null;
                        }
                        // Append the character
                        tStr += ch;
                        // Set flag
                        inVarEvl = true;
                        // Go back to top
                        continue;
                    }
                }
                // Check for '}' or  ']' after "${" or "$["
                if( inVarEvl && (ch == '}' || ch == ']') ) {
                    // Append the character
                    tStr += ch;
                    // Store the token as needed
                    if( tStr.length() > 0 ) {
                        // Check for empty regular expression
                        if( tRX1.isEmpty() && !tRX2.isEmpty() ) eStr = Texts.EMsg_InvalidRegExp;
                        // Check the regular variable name

                        if( tStr.charAt(1) == '{' ) {
                            // Remove whitespaces between "${" or "$[", the variable name, and "$}" or "$]"
                            tStr = _reTrimWSInVarName.matcher(tStr).replaceFirst​("$1$2$3");
                            // Check if it is a valid symbol name
                            if( !XCom.isSymbolName( XCom.trmRVarName(tStr), true ) ) eStr = XCom.errorString( Texts.EMsg_InvalidVarName, XCom.trmRVarName(tStr) );
                        }
                        // Create the token
                        final Token t = new Token(token.path, token.lNum, token.cNum, tStr, tRX1, tRX2, eStr, false);
                        // Check for ~${... ~$[... and ^${... ^$[... and -${... -$[...
                        if(fcsChar != 0) {
                            t.cFCS  = fcsChar;
                            fcsChar = 0;
                        }
                        // Store the token
                        tokens.add(t);
                        // Clear variables
                        tStr = "";
                        tRX1 = "";
                        tRX2 = "";
                        eStr = null;
                    }
                    // Reset flag
                    inVarEvl = false;
                    // Go back to top
                    continue;
                }
                // Check for the beginning of ~${... ~$[...
                if(ch == '~') {
                    if( (i + 1) < lineStrLen && token.tStr.charAt(i + 1) == '$' ) {
                        fcsChar = '~';
                        continue;
                    }
                }
                // Check for the beginning of ^${... ^$[...
                else if(ch == '^') {
                    if( (i + 1) < lineStrLen && token.tStr.charAt(i + 1) == '$' ) {
                        fcsChar = '^';
                        continue;
                    }
                }
                // Check for the beginning of -${... -$[...
                else if(ch == '-') {
                    if( (i + 1) < lineStrLen && token.tStr.charAt(i + 1) == '$' ) {
                        fcsChar = '-';
                        continue;
                    }
                }
                // Check for '/' after "${" or "$["
                if(inVarEvl && ch == '/') {
                    inRegExp = 1;
                    continue; // Go back to top
                }
                // Append the character
                tStr += ch;
            }

        } // for i

        // Store the last token as needed
        if( tStr.length() > 0 ) {
            /*
            // Check for empty regular expression
            if( tRX1.isEmpty() && !tRX2.isEmpty() ) eStr = Texts.EMsg_InvalidRegExp;
            // Store the token
            tokens.add( new Token(token.path, token.lNum, token.cNum, "'" + tStr + "'", tRX1, tRX2, eStr, true) );
            */
            // Store the token (at this point, it should be a constant)
            if( !tRX1.isEmpty() || !tRX2.isEmpty() ) eStr = XCom.errorString(Texts.EMsg_UnexpectedToken, tStr);
            tokens.add( new Token(token.path, token.lNum, token.cNum, "'" + tStr + "'", "", "", eStr, true ) );
        }

        // Return the tokens
        return tokens;
    }

} // class TokenReader
