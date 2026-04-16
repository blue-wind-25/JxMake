/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;

import java.util.regex.Pattern;

import jxm.*;


public class XLoader {

    public static final Pattern loadPattern(final DataInputStream dis) throws IOException
    {
        // If the flag is false, return null
        if( XCacheHelper.loadIfNull(dis) ) return null;

        // Read the data and then create and return a new instance
        return Pattern.compile( dis.readUTF() );
    }

    public static final String loadString(final DataInputStream dis) throws IOException
    {
        // If the flag is false, return null
        if( XCacheHelper.loadIfNull(dis) ) return null;

        // Read the data and then create and return a new instance
        return dis.readUTF();
    }

    public static ArrayList<String> loadArrayListString(final DataInputStream dis) throws Exception
    {
        // Read the number of items
        final int rvsCount = dis.readInt();

        // Create a new instance
        final ArrayList<String> arrayListString = new ArrayList<>();

        // Read the data and then create and store the new list member(s)
        for(int i = 0; i < rvsCount; ++i) arrayListString.add( loadString(dis) );

        // Return the newly created instance
        return arrayListString;
    }

    public static XCom.ASNSpec loadASNSpec(final DataInputStream dis) throws Exception
    {
        // Read the data and return the appropriate instance
        switch( dis.readByte() ) {
            case XCom.ASNSpecNumCode_lazy            : return XCom.ASNSpec_lazy;
            case XCom.ASNSpecNumCode_lazy_concat     : return XCom.ASNSpec_lazy_concat;
            case XCom.ASNSpecNumCode_lazy_ifNotSet   : return XCom.ASNSpec_lazy_ifNotSet;
            case XCom.ASNSpecNumCode_direct          : return XCom.ASNSpec_direct;
            case XCom.ASNSpecNumCode_direct_concat   : return XCom.ASNSpec_direct_concat;
            case XCom.ASNSpecNumCode_direct_ifNotSet : return XCom.ASNSpec_direct_ifNotSet;
        }

        // Invalid data
        return null;
    }

    public static XCom.CompareType loadCompareType(final DataInputStream dis) throws IOException
    { return XCom.CompareType.valueOf( dis.readUTF() ); }

    public static XCom.FuncSpec loadFuncSpec(final DataInputStream dis) throws Exception
    { return XCom.getFuncSpec( dis.readUTF() ); }

    public static XCom.SVarSpec loadSVarSpec(final DataInputStream dis) throws Exception
    {
        // If the flag is false, return null
        if( XCacheHelper.loadIfNull(dis) ) return null;

        // Read the data and then create and return a new instance
        return XCom.getSVarSpec( dis.readUTF() );
    }

    public static XCom.ALOperName loadALOperName(final DataInputStream dis) throws IOException
    { return XCom.ALOperName.valueOf( dis.readUTF() ); }

    public static XCom.ALOperSpec loadALOperSpec(final DataInputStream dis) throws Exception
    { return XCom.getALOperSpec( dis.readUTF() ); }

    public static XCom.ReadVarSpec loadReadVarSpec(final DataInputStream dis) throws Exception
    {
        // Return 'XCom.RVSpec_EmptyString' as needed
        if( !dis.readBoolean() ) return XCom.RVSpec_EmptyString;

        // Read the data
        final boolean       constant    = dis.readBoolean();
        final String        value       = loadString  (dis);
        final Pattern       regexp      = loadPattern (dis);
        final String        replacement = loadString  (dis);
        final XCom.SVarSpec svarSpec    = loadSVarSpec(dis);

        // Check if it is not a compile-time constant
        final boolean nctConst = (svarSpec != null) && !svarSpec.svarName.isCompileTimeConstant();

        // Create and return a new instance
        return new XCom.ReadVarSpec(
             constant, svarSpec, nctConst ? svarSpec.constVal : value, regexp, replacement
        );
    }

    public static XCom.ReadVarSpecs loadReadVarSpecs(final DataInputStream dis) throws Exception
    {
        // Return 'XCom.RVSpcs_EmptyString' as needed
        if( !dis.readBoolean() ) return XCom.RVSpcs_EmptyString;

        // Read the number of items
        final int rvsCount = dis.readInt();

        // Check if the list is empty
        if(rvsCount == 0) return XCom.RVSpcs_EmptyList;

        // Create a new instance
        final XCom.ReadVarSpecs readVarSpecs = new XCom.ReadVarSpecs();

        // Read the data and then create and store the new list member(s)
        for(int i = 0; i < rvsCount; ++i) readVarSpecs.add( loadReadVarSpec(dis) );

        // Return the newly created instance
        return readVarSpecs;
    }

    public static XCom.LabelMap loadLabelMap(final DataInputStream dis) throws IOException
    {
        // Return a null label map as needed
        if( !dis.readBoolean() ) return null;

        // Create a new instance
        final XCom.LabelMap labelMap = new XCom.LabelMap();

        // Read the number of items
        final int lbmCount = dis.readInt();

        // Read the data
        for(int i = 0; i < lbmCount; ++i) {
            final String k = dis.readUTF();
            final int    v = dis.readInt();
            labelMap.put(k, v);
        }

        // Return the newly created instance
        return labelMap;
    }

    public static XCom.VariableStore loadVariableStore(final DataInputStream dis) throws Exception
    {
        // Return 'XCom.VarStr_EmptyString' as needed
        if( !dis.readBoolean() ) return XCom.VarStr_EmptyString;

        // Read the data
        final boolean constant    = dis.readBoolean();
        final String  value       = loadString  (dis);
        final Pattern regexp      = loadPattern (dis);
        final String  replacement = loadString  (dis);

        // Create and return a new instance
        return new XCom.VariableStore(constant, value, regexp, replacement);
    }

    public static XCom.VariableValue loadVariableValue(final DataInputStream dis) throws Exception
    {
        // Return 'XCom.VarVal_EmptyString' as needed
        if( !dis.readBoolean() ) return XCom.VarVal_EmptyString;

        // Read the '_const' flag
        final boolean const_ = dis.readBoolean();

        // Read the number of items
        final int vvCount = dis.readInt();

        // Create a new instance
        final XCom.VariableValue variableValue = new XCom.VariableValue();

        // Read the data and then create and store the new list member(s)
        for(int i = 0; i < vvCount; ++i) variableValue.add( loadVariableStore(dis) );

        // Set the '_const' flag
        if(const_) variableValue.setConstantVariable();

        // Return the newly created instance
        return variableValue;
    }

    public static XCom.PostfixTerm loadPostfixTerm(final DataInputStream dis) throws Exception
    {
        // Read the data and create and return a new instance
        if( XCacheHelper.loadIfNull(dis) ) {
            return new XCom.PostfixTerm( XCom.PostfixOper.valueOf( dis.readUTF() ) );
        }
        else {
            return new XCom.PostfixTerm( XLoader.loadReadVarSpec(dis) );
        }
    }

    public static XCom.PostfixTerms loadPostfixTerms(final DataInputStream dis) throws Exception
    {
        // Read the number of items
        final int ptCount = dis.readInt();

        // Create a new instance
        final XCom.PostfixTerms postfixTerms = new XCom.PostfixTerms();

        // Read the data and then create and store the new list member(s)
        for(int i = 0; i < ptCount; ++i) postfixTerms.add( loadPostfixTerm(dis) );

        // Return the newly created instance
        return postfixTerms;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ArrayList<TokenReader.Token> loadTRTokens(final DataInputStream dis) throws IOException
    {
        final ArrayList<TokenReader.Token> trTokens = new ArrayList<>();

        final int tCnt = dis.readInt();

        for(int i = 0; i < tCnt; ++i) trTokens.add( TokenReader.Token.loadFromStream(dis) );

        return trTokens;
    }

    public static void loadMacroDefs(final DataInputStream dis, final SpecReader.MacroDefs macroDefs) throws IOException
    {
        if( macroDefs == null || XCacheHelper.loadIfNull(dis) ) return;

        final int mCnt = dis.readInt();

        for(int i = 0; i < mCnt; ++i) {
            final String name = dis.readUTF();
            macroDefs.put( name, loadTRTokens(dis) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static ArrayList<String> _loadDepListFromFile(final String absPath)
    {
        // The file list
        final ArrayList<String> allSpecFileList = new ArrayList<>();

        // Try to load the dependency list
        try {
            // Open the file
            final BufferedReader bfr = new BufferedReader( new InputStreamReader( new FileInputStream(absPath), SysUtil._CharEncoding ) );
            // Read the data
            while(true) {
                // Read one line
                final String line = bfr.readLine();
                if(line == null) break;
                // Store the line is it is not empty
                if( line.length() != 0 ) allSpecFileList.add(line);
            }
            // Close the file
            bfr.close();
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Return error
            return null;
        }

        // Done
        return allSpecFileList;
    }

    private static XCom.ExecBlocks _loadFromBinFile(final String cspecBinFileAbsPath, final SpecReader.MacroDefs macroDefs, final boolean forCache)
    {
        // Load the data
        XCom.ExecBlocks execBlocks = null;

        // Load the compiled specification
        try(
            // Open the file
            final DataInputStream dis = new DataInputStream( new BufferedInputStream( new FileInputStream(cspecBinFileAbsPath) ) );
        ) {
            // Load the header
            if( !XCacheHelper.loadCSpecFileHeader(dis) ) {
                if( forCache && XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XLoadJxMakeCacheInvalid);
                return null;
            }
            // Load and return the execution blocks
            execBlocks = XCacheHelper.loadExecBlocks(dis);
            // Load the macro definitions
            loadMacroDefs(dis, macroDefs);
            // There is no need to call 'dis.close()' because it is a auto-closeable resource
        }
        catch(final Exception e) {
            // Print message as needed
            if( forCache && XCacheHelper.getVerboseMode() ) {
                SysUtil.printf_stdErr( "%s\n", SysUtil.stringFromStackTrace(e) );
                SysUtil.printf_stdErr(Texts.IMsg_XLoadJxMakeCacheLoadFail);
            }
            else {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
            // Return error
            return null;
        }

        if( XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XLoadJxMakeCacheLoadDone);

        // Done
        return execBlocks;
    }

    public static XCom.ExecBlocks loadFromCacheFile(final String mainJXMSpecFile_absPath)
    {
        // Get the compiled specification cache file path
        final String cspecFilePath = XCacheHelper.getCSpecFilePath(mainJXMSpecFile_absPath);

        if(cspecFilePath == null) {
            if( XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XLoadJxMakeNoSpecCachePath);
            return null;
        }

        // Get the compiled specification cache file dependency list file path
        final String cspecFilePath_depList = SysUtil.getCacheFileName_compiledSpecFile_dependencyList(cspecFilePath);

        // Load the dependency list
        final ArrayList<String> allSpecFileList = _loadDepListFromFile(cspecFilePath_depList);
        if(allSpecFileList == null) {
            if( XCacheHelper.getVerboseMode() ) {
                if( !SysUtil.pathIsValid(cspecFilePath_depList) ) {
                    SysUtil.printf_stdErr(Texts.IMsg_XLoadJxMakeSpecCacheNoDF);
                }
                else {
                    SysUtil.printf_stdErr(Texts.IMsg_XLoadJxMakeSpecCacheDFLoadFail);
                }
            }
            return null;
        }

        if( XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XLoadJxMakeSpecCacheDFLoadDone);

        // Check if the cache is up to date
        if( !SysUtil.isPathMoreRecent(cspecFilePath, allSpecFileList) ) {
            if( XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XLoadJxMakeSpecCacheDFOutOfDate);
            return null;
        }

        if( XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XLoadJxMakeSpecCacheDFUpToDate);

        // Load the compiled specification binary data
        return _loadFromBinFile(cspecFilePath, null, true);
    }

    public static XCom.ExecBlocks loadFromLibraryFile(final String cspecBinFileAbsPath, final SpecReader.MacroDefs macroDefs)
    { return _loadFromBinFile(cspecBinFileAbsPath, macroDefs, false); }

} // class XLoader
