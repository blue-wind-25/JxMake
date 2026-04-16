/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Collectors;

import jxm.*;
import jxm.xb.*;


public class JavaSP {

    private final String             _srcFileName;
    private       int                _srcLineNumber = 0;

    private final BufferedReader     _srcFileReader;
    private final BufferedWriter     _dstFileWriter;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _dumpTextFile(final String file) throws Exception
    {
        final BufferedReader bfr = new BufferedReader( new InputStreamReader ( new FileInputStream (file), SysUtil._CharEncoding ) );

        while(true) {
            final String line = bfr.readLine();
            if(line == null) break;
            SysUtil.stdDbg().println(line);
        }

        bfr.close();
    }

    private String _readLine() throws Exception
    {
        final String line = _srcFileReader.readLine();

        if(line == null) return null;

        ++_srcLineNumber;

        return line;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public JavaSP(final String srcFilePath, final String dstFilePath) throws Exception
    {
        _srcFileName   = SysUtil.getFileName(srcFilePath);

        _srcFileReader = new BufferedReader( new InputStreamReader ( new FileInputStream (srcFilePath), SysUtil._CharEncoding ) );
        _dstFileWriter = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(dstFilePath), SysUtil._CharEncoding ) );
    }

    public void process(final Set<String> definedCFlags, final Map<String, String> definedSVals) throws Exception
    {
        // Write the warning text
        _dstFileWriter.write( String.format(Texts.IMsg_JSP_GeneratedFile, _srcFileName) );

        // Quote the substitution value(s)
        final Map<String, String> quotedSVals = definedSVals.entrySet().stream().collect( Collectors.toMap(
                                                    Map.Entry::getKey,
                                                    e -> Matcher.quoteReplacement( e.getValue() )
                                                ) );

        // Process the file
        _process(definedCFlags, quotedSVals);

        // Close the reader and writer
        _srcFileReader.close();
        _dstFileWriter.close();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _throwInvalidDirective(final String token) throws JXMException
    { throw XCom.newJXMException(Texts.EMsg_JSP_InvalidDirective, _srcFileName, _srcLineNumber, token); }

    private void _throwInvalidNumOfTokens(final String token) throws JXMException
    { throw XCom.newJXMException(Texts.EMsg_JSP_InvalidNumOfTokens, _srcFileName, _srcLineNumber, token); }

    private void _throwInvalidSymbolName(final String token) throws JXMException
    { throw XCom.newJXMException(Texts.EMsg_JSP_InvalidSymbolName, _srcFileName, _srcLineNumber, token); }

    private void _throwXXXWithoutIf(final String token) throws JXMException
    { throw XCom.newJXMException(Texts.EMsg_JSP_XXXWithoutIf, _srcFileName, _srcLineNumber, token); }

    private void _throwUnterminatedXXX(final String token) throws JXMException
    { throw XCom.newJXMException(Texts.EMsg_JSP_UnterminatedXXX, _srcFileName, _srcLineNumber, token); }

    private void _throwUndefinedSubst(final String token) throws JXMException
    { throw XCom.newJXMException(Texts.EMsg_JSP_UndefinedSubst, _srcFileName, _srcLineNumber, token); }

    private void _throwUsingMLCUnsupported() throws JXMException
    { throw XCom.newJXMException(Texts.EMsg_JSP_UsingMLCUnsupported, _srcFileName, _srcLineNumber); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static enum CBType {
        _If   ("#if"   , 2),
        _Else ("#else" , 1),
        _EndIf("#endif", 1),

        // Invalid type
        __INVALID__(null, 0)

        ;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private final String _strName;
        private final int    _reqTCnt;

        private CBType(final String strName, final int reqTCnt)
        {
            _strName = strName;
            _reqTCnt = reqTCnt;
        }

        private String strName() { return _strName; }
        private int    reqTCnt() { return _reqTCnt; }
    }

    private static class CBState {
        private final CBType  cbType;
        private final boolean cbValue;

        private CBState(final CBType cbType_, final boolean cbValue_)
        {
            cbType  = cbType_;
            cbValue = cbValue_;
        }
    };

    @SuppressWarnings("serial")
    private class CBStateStack extends ArrayList<CBState> {
        private void _check(final CBType cbType, final int gotTCnt) throws Exception
        {
            if( cbType  != CBType._If && this.isEmpty() ) _throwXXXWithoutIf      ( cbType.strName() );
            if( gotTCnt != cbType.reqTCnt()             ) _throwInvalidNumOfTokens( cbType.strName() );
        }

        private String _cbLastStrName()
        { return this.isEmpty() ? "" : this.get( this.size() - 1 ).cbType.strName(); }

        private boolean _cbPrevValue()
        { return ( this.size() < 2 ) ? true : this.get( this.size() - 2 ).cbValue; }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private void processIf(final int gotTCnt, final boolean cbValue) throws Exception
        {
            _check(CBType._If, gotTCnt);

            final CBState element = new CBState( CBType._If, cbLastValue() & cbValue );
            this.add(element);
        }

        private void processElse(final int gotTCnt) throws Exception
        {
            _check(CBType._Else, gotTCnt);

            final CBState element = new CBState( CBType._Else, _cbPrevValue() & !cbLastValue() );
            this.add(element);
        }

        private void processEndIf(final int gotTCnt) throws Exception
        {
            _check(CBType._EndIf, gotTCnt);

            while(true) {
                final CBState element = this.remove( this.size() - 1 );
                if(element.cbType == CBType._If) break;
            }
        }

        private void end() throws Exception
        {
            // Error if there is an unterminated condition block
            if( !this.isEmpty() ) _throwUnterminatedXXX( this._cbLastStrName() );
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private boolean cbLastValue()
        { return this.isEmpty() ? true : this.get( this.size() - 1 ).cbValue; }
    }

    private CBStateStack _cbStateStack = new CBStateStack();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _pmSubstSymbol = Pattern.compile("#subst\\{\\s*(" + XCom._reStrSymbolNameUnicode + ")\\s*\\}", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern _pmSubstAny    = Pattern.compile("#subst\\{\\s*(.*)\\s*\\}");

    private static Boolean _evalBICondStr(final String str)
    {
        final String strLC = str.toLowerCase();

        if( strLC.equals("true" ) ) return Boolean.TRUE;
        if( strLC.equals("false") ) return Boolean.FALSE;

        try {
            return ( Long.decode(str) != 0 ) ? Boolean.TRUE : Boolean.FALSE;
        }
        catch(final Exception e) {}

        return null;
    }

    public void _process(final Set<String> definedCFlags, final Map<String, String> definedSVals) throws Exception
    {
        // Flag
        boolean insideMLComment = false;

        // Process the lines
        while(true) {

            // Read one line
            String line = _readLine();

            if(line == null) {
                _cbStateStack.end();
                break;
            }

            // Check if currently inside a multiline comment
            boolean dirInsideMLComment = false;

            if(insideMLComment) {
                final int idxEMLC = line.indexOf("*/");
                final int idxDBC  = line.indexOf("#" );
                if(idxEMLC >= 0) {
                    // Reset the multiline comment flag
                    insideMLComment = false;
                    // Ensure the direction after the end of the comment will be processed later
                    if( idxDBC > (idxEMLC + 2) && !line.isEmpty() ) {
                        if( line.substring(idxEMLC + 2, idxDBC).trim().isEmpty() ) {
                            line = line.substring( idxDBC, line.length() );
                        }
                    }
                    // Check if the direective is inside a multiline comment
                    if(idxDBC < idxEMLC) dirInsideMLComment = true;
                }
                else if(idxDBC < 0) {
                    if( _cbStateStack.cbLastValue() ) {
                        _dstFileWriter.write  (line);
                        _dstFileWriter.newLine(    );
                    }
                    continue;
                }
            }

            // Create a stripped line (remove all comments and trim the line)
            String  sline = (insideMLComment || dirInsideMLComment) ? "" : line;
            boolean gotCM = false;
            int     cntCM = 0;

            do {
                // Reset the loop flag
                gotCM = false;
                // Store only the string before '/*'
                final int    idxBMLC = sline.indexOf("/*");
                      String bStr    = "";
                if(idxBMLC >= 0) {
                    // Set the loop flag
                    gotCM = true;
                    // Set the multiline comment flag
                    insideMLComment = true;
                    // Increment the counter
                    ++cntCM;
                    // Store the substring
                    bStr = sline.substring(0, idxBMLC);
                }
                // Store only the string after '*/'
                final int    idxEMLC = sline.indexOf("*/");
                      String aStr    = "";
                if(idxEMLC >= 0) {
                    // Set the loop flag
                    gotCM = true;
                    // Reset the multiline comment flag
                    insideMLComment = false;
                    // Decrement the counter
                    --cntCM;
                    // Store the substring
                    final int idxXMLC = idxEMLC + 2;
                    final int lenLine = sline.length();
                    aStr = (lenLine > idxEMLC) ? sline.substring(idxXMLC, lenLine) : "";
                }
                // Combine the stored strings as needed
                if(gotCM) sline = bStr + aStr;
            } while(gotCM);

            final int idxSLC  = sline.indexOf("//");
            if(idxSLC >= 0) {
                // Store only the string before '//'
                sline = sline.substring(0, idxSLC);
            }

            sline = sline.trim(); // Trim the string

            // Extract the condition token(s)
            final String[] tokens = ( !sline.isEmpty() && sline.charAt(0) == '#' ) ? XCom.re_splitWhitespaces(sline) : null;

            if(tokens != null && cntCM != 0) _throwUsingMLCUnsupported();

            // Process the condition token(s)
            if(tokens != null) {
                // Get the number of tokens and condition string
                int    tCnt = tokens.length;
                String cStr = (tCnt >= 2) ? tokens[1] : "";
                if( tCnt >= 3 && cStr.equals("!") ) {
                    // Combine separated '!' to the condition string
                    cStr += tokens[2];
                    --tCnt;
                }
                // Check if the condition is inverted
                final boolean cInv = !cStr.isEmpty() && ( cStr.charAt(0) == '!' );
                if(cInv) cStr = cStr.substring(1);
                // Check the condition string against built-in rules
                Boolean cRes = _evalBICondStr(cStr);
                if(cRes == null) {
                    if( !cStr.isEmpty() ) {
                        if( !XCom.isSymbolName(cStr) ) _throwInvalidSymbolName(cStr);
                        cRes = definedCFlags.contains(cStr);
                    }
                    else {
                        cRes = false;
                    }
                }
                if(cInv) cRes = !cRes;
                // Process the condition block
                     if( tokens[0].equals("#if"   ) ) _cbStateStack.processIf   (tCnt, cRes);
                else if( tokens[0].equals("#else" ) ) _cbStateStack.processElse (tCnt      );
                else if( tokens[0].equals("#endif") ) _cbStateStack.processEndIf(tCnt      );
            }

            // Output the line as needed
            final boolean noOutput = (tokens != null) || !_cbStateStack.cbLastValue();

            if(noOutput) {
                // Do not output the line if it is a directive or disabled line
                /*
                _dstFileWriter.write("//");
                _dstFileWriter.write(line);
                _dstFileWriter.newLine();
                //*/
            }
            else {
                try {
                    // Perform substitution(s)
                    final String xline = XCom.re_replace(line, _pmSubstSymbol, definedSVals);
                    // Check for invalid symbol name(s)
                    final Matcher m = _pmSubstAny.matcher(xline);
                    while( m.find() ) _throwInvalidSymbolName( m.group(1) );
                    // Write the result
                    _dstFileWriter.write(xline);
                    _dstFileWriter.newLine();
                }
                catch(final IllegalArgumentException e) {
                    _throwUndefinedSubst( e.getMessage() );
                }
            }

        } // while
    }

} // class JavaSP
