/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import java.nio.file.attribute.FileTime;

import jxm.xb.*;


//
// Specification file(s) reader class
//
public class SpecReader {

    private static final HashSet<String> _onceList = new HashSet<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ArrayList<String     > _fileList       = new ArrayList<>();
    private final Stack    <TokenReader> _trStack        = new Stack    <>();

    private       FileTime               _latestFileTime = null;

    private       XCom.ExecBlocks        _xbLib          = null;

    private final boolean                _verboseLibLoad;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class MacroDefs extends HashMap< String, ArrayList<TokenReader.Token> > {}

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SpecReader(final boolean verboseLibLoad)
    { _verboseLibLoad = verboseLibLoad; }

    public FileTime getLatestFileTime()
    { return _latestFileTime; }

    public boolean isLibBinFileLoaded()
    { return _xbLib != null; }

    public XCom.ExecBlocks getLibBinFileExecBlocks()
    {
        final XCom.ExecBlocks xb = _xbLib;

        _xbLib = null;

        return xb;
    }

    public MacroDefs pushSpecFile(final String jxmSpecFile_relPath_or_absPath, boolean skipIfNotExist, boolean once) throws IOException
    {
        // For loading macro definitions
        final MacroDefs macroDefs = new MacroDefs();

        // Resolve the path
        String  filePath    = jxmSpecFile_relPath_or_absPath;
        boolean isLibFile   = false;
        boolean libNotFound = false;
        boolean fromJAR     = false;

        if( filePath.charAt(0) == '<' && filePath.charAt( filePath.length() - 1 ) == '>' ) {
            // Set flag
            isLibFile = true;
            // Find the absolute path of the library file
            final String lfp = SysUtil.findJXMLibAbsFilePath( filePath.substring( 1, filePath.length() - 1 ) ); // Remove the enclosing '<' and '>'
            if(lfp != null) filePath    = lfp;
            else            libNotFound = true;
            // Check if it is a resource from the JAR file
            if(!libNotFound) fromJAR = filePath.startsWith(SysUtil._JxMakeProgramJARResPrefix);
            // Check if it must only be included once
            if( !libNotFound && once && _onceList.contains(filePath) ) {
                SysUtil.stdOut().printf(Texts.IMsg_SReadSkipLoadJxMakeLib, filePath);
                return null;
            }
            // Check if the precompiled version of the library file exists and readable
            if(!libNotFound && !fromJAR) {
                final String binFileAbsPath = filePath + SysUtil._JxMakeProgramBinFileExt;
                if( SysUtil.pathIsReadableFile(binFileAbsPath) ) {
                    // Check if the library source file is actually more recent than its precompiled version
                    if( SysUtil.isPathMoreRecent(filePath, binFileAbsPath) ) {
                        // Show message as needed
                        if(_verboseLibLoad) {
                            SysUtil.stdOut().printf(Texts.IMsg_SReadSkipLoadJxMakePCLib, binFileAbsPath);
                        }
                    }
                    // The precompiled version is up to date
                    else {
                        // Show message as needed
                        if(_verboseLibLoad) {
                            SysUtil.stdOut().printf(Texts.IMsg_SReadLoadJxMakePCLib, binFileAbsPath);
                        }
                        // Load from precompiled library
                        final XCom.ExecBlocks execBlocks = XLoader.loadFromLibraryFile(binFileAbsPath, macroDefs);
                        // Check if the loading completed successfully
                        if(execBlocks != null) {
                            // Show message as needed
                            if(_verboseLibLoad) {
                                SysUtil.stdOut().println(Texts.IMsg_SReadLoadJxMakePCLibDone);
                            }
                            // Store the execution blocks
                            _xbLib = execBlocks;
                            // Save the latest file time
                            final FileTime chkFileTime = SysUtil.pathGetTime(binFileAbsPath);
                            if( _latestFileTime == null || chkFileTime.compareTo(_latestFileTime) > 0 ) _latestFileTime = chkFileTime;
                            // Store to the list
                            if( !_fileList.contains(binFileAbsPath) ) _fileList.add(binFileAbsPath);
                            // Store to the once list as needed
                            if(once) _onceList.add(filePath);
                            // Return the macro definitions
                            return macroDefs.isEmpty() ? null : macroDefs;
                        }
                        // Loading Error
                        else {
                            if(_verboseLibLoad) {
                                SysUtil.stdOut().println(Texts.IMsg_SReadLoadJxMakePCLibFail);
                            }
                            else {
                                SysUtil.printSuppressedError( null, 0, 0, String.format(Texts.IMsg_SReadLoadJxMakePCLibSupErr, binFileAbsPath) );
                            }
                        }
                    }
                }
            }
        }

        else if( !_trStack.empty() ) {
            // If the parent token reader loads its file from the JAR file resources,
            // try to load this file from the JAR file resources first
            if( _trStack.peek().isFromJAR() ) {
                final String lfp = SysUtil.findJXMLibAbsFilePath( filePath.substring( 0, filePath.length() - 4 ) ); // Remove the '.jxm' extension
                if(lfp != null) {
                                fromJAR  = lfp.startsWith(SysUtil._JxMakeProgramJARResPrefix);
                    if(fromJAR) filePath = lfp;
                }
            }
            // Next, resolve the absolute path of this file using the absolute path of the last specification file
            if(!fromJAR) {
                final String dirPath = SysUtil.getDirName( _trStack.peek().inputJXMSpecFile_absPath() );
                filePath = SysUtil.resolvePath(filePath, dirPath);
            }
        }

        // Store to the list
        if( !_fileList.contains(filePath) ) _fileList.add(filePath);

        // If the 'skipIfNotExist' flag is set, simply return if the file does not actually exist/readable
        if(skipIfNotExist) {
            if( libNotFound || !SysUtil.pathIsReadableFile(filePath) ) return null;
        }

        // Show message as needed
        if(isLibFile && _verboseLibLoad) {
            if( filePath.charAt(0) == '<' ) {
                SysUtil.stdOut().printf(Texts.IMsg_SReadInvalidJxMakeLib, filePath);
            }
            else {
                SysUtil.stdOut().printf(Texts.IMsg_SReadLoadJxMakeLib, filePath);
            }
        }

        // Check if it must only be included once
        if( once && _onceList.contains(filePath) ) return null;

        // Store to the once list as needed
        if(once) _onceList.add(filePath);

        // Create and store a new token reader
        _trStack.push( new TokenReader(filePath, fromJAR) );

        // Save the latest file time
        final FileTime chkFileTime = SysUtil.pathGetTime( fromJAR ? SysUtil.getJxMakeJarFile_noexcept().getPath() : SysUtil.resolveAbsolutePath(filePath) );
        if( _latestFileTime == null || chkFileTime.compareTo(_latestFileTime) > 0 ) _latestFileTime = chkFileTime;

        // Return the macro definitions
        return macroDefs.isEmpty() ? null : macroDefs;
    }

    public ArrayList<String> getAllSpecFileList()
    { return _fileList; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void injectTokens(final ArrayList<TokenReader.Token> tokens)
    { _trStack.peek().injectTokens(tokens); }

    public void injectTokens(final TokenReader.Token... tokens)
    { _trStack.peek().injectTokens(tokens); }

    public void injectTokens(final ArrayList<TokenReader.Token> tokens, final TokenReader.Token... extraTokens)
    { _trStack.peek().injectTokens(tokens, extraTokens); }

    public void injectToken_EOL(final TokenReader.Token refPLCToken)
    { _trStack.peek().injectToken_EOL(refPLCToken); }

    public void injectToken_Colon(final TokenReader.Token refPLCToken)
    { _trStack.peek().injectToken_Colon(refPLCToken); }

    public void injectToken_endif(final TokenReader.Token refPLCToken)
    { _trStack.peek().injectToken_endif(refPLCToken); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void injectMacroTokens(final ArrayList<TokenReader.Token> tokens)
    { _trStack.peek().injectMacroTokens(tokens); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getSpecFilePathHash()
    { return _trStack.peek().getSpecFilePathHash(); }

    public TokenReader.Token readToken(final XBBuilder xbBuilder) throws IOException
    {
        while(true) {

            // Break if the stack is empty
            if( _trStack.empty() ) break;

            // Try to read token from the stack top
            final TokenReader.Token token = _trStack.peek().readToken();

            // If there is no token to read, pop the stack and repeat
            if(token == null) {
                _trStack.pop();
                continue;
            }

            // Process compiler directives
            if( !xbBuilder.inMLComment() && token.tStr.startsWith(":::") ) {
                // Error if a one-line if statement is in progress
                if( xbBuilder.inOneLineIf() ) {
                    token.eStr = XCom.errorString(Texts.EMsg_TokenNoFollowOneLineIf, token.tStr);
                    return token;
                }
                // Process ':::pragma'
                if( token.tStr.equals(":::pragma" ) ) {
                    // Get and check the arguments
                    final TokenReader.Token pragmaType  = _trStack.peek().readToken();
                    final TokenReader.Token pragmaParam = _trStack.peek().readToken();
                    final TokenReader.Token tokenEOL    = _trStack.peek().readToken();
                    try {
                        if( pragmaType.isEOL() || pragmaParam == null || pragmaParam.isEOL() || tokenEOL == null ) {
                            final TokenReader.Token errToken = ( pragmaType.isEOL() || pragmaParam == null ) ? pragmaType : pragmaParam;
                            errToken.eStr = XCom.errorString(Texts.EMsg_PrematureEOL);
                            return errToken;
                        }
                        else if( !tokenEOL.isEOL() ) {
                            tokenEOL.eStr = XCom.errorString(Texts.EMsg_UnexpectedExtraToken, tokenEOL.tStr);
                            return tokenEOL;
                        }
                        else if( pragmaType.tStr.equals("jxmake_compile_minimum_version") ) {
                            if( !SysUtil.jxmCompileCheckMinVer(pragmaParam.tStr) ) {
                                pragmaParam.eStr = XCom.errorString(Texts.EMsg_JxMakeMinVer, pragmaParam.tStr);
                                return pragmaParam;
                            }
                        }
                        else if( pragmaType.tStr.equals("jxmake_compile_maximum_version") ) {
                            if( !SysUtil.jxmCompileCheckMaxVer(pragmaParam.tStr) ) {
                                pragmaParam.eStr = XCom.errorString(Texts.EMsg_JxMakeMaxVer, pragmaParam.tStr);
                                return pragmaParam;
                            }
                        }
                        else {
                            pragmaType.eStr = XCom.errorString(Texts.EMsg_UnsupportedPragma, pragmaType.tStr);
                            return pragmaType;
                        }
                    }
                    catch(final Exception e) {
                        pragmaParam.eStr = XCom.errorString( e.toString(), pragmaParam.tStr );
                        return pragmaParam;
                    }
                    // Done processing the pragma
                    continue;
                }
                // Process ':::include' and ':::sinclude'
                else {
                    final boolean gotInclude  = token.tStr.equals(":::include" ) || token.tStr.equals(":::include_once" );
                    final boolean gotSInclude = token.tStr.equals(":::sinclude") || token.tStr.equals(":::sinclude_once");
                    final boolean gotOnce     = token.tStr.endsWith("_once");
                    if(gotInclude || gotSInclude) {
                        // Get and check the arguments
                        final TokenReader.Token tokenFileName = _trStack.peek().readToken();
                        final TokenReader.Token tokenEOL      = _trStack.peek().readToken();
                        if( tokenFileName.isEOL() || tokenEOL == null ) {
                            tokenFileName.eStr = XCom.errorString(Texts.EMsg_PrematureEOL);
                            return tokenFileName;
                        }
                        else if( !tokenEOL.isEOL() ) {
                            tokenEOL.eStr = XCom.errorString(Texts.EMsg_UnexpectedExtraToken, tokenEOL.tStr);
                            return tokenEOL;
                        }
                        else if( tokenFileName.isDQString() || tokenFileName.isRVarSpec() || tokenFileName.isSVarSpec() ) {
                            tokenFileName.eStr = XCom.errorString(Texts.EMsg_UnexpectedToken, tokenFileName.tStr);
                            return tokenFileName;
                        }
                        // Get the file name
                        final String sfn = TokenReader.unquoteSQString(tokenFileName).tStr;
                        // Push the new specification file name
                        try {
                            // Push the specification file name
                            xbBuilder.addMacroDefs( pushSpecFile(sfn, gotSInclude, gotOnce) );
                            // Check if a compiled library file has been loaded
                            if( isLibBinFileLoaded() ) return null;

                        }
                        catch(final IOException | JXMException e) {
                            // Print the stack trace if requested
                            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                            // Set the error message to the token
                            token.eStr = XCom.errorString( e.toString() );
                            return token;
                        }
                        // Repeat with the newly pushed specification file
                        continue;
                    } // if
                } // if
            } // if

            // Return the token
            return token;

        } // while true

        // No more token can be read
        return null;
    }

} // class SpecReader
