/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.Map;

import java.util.regex.Pattern;

import jxm.*;


public class XSaver {

    public static final void savePattern(final DataOutputStream dos, final Pattern obj) throws IOException
    {
        // Write false if it is null; write true otherwise
        if( XCacheHelper.saveIfNull(dos, obj) ) return;

        // Write the data
        dos.writeUTF( obj.pattern() );
    }

    public static final void saveString(final DataOutputStream dos, final String obj) throws IOException
    {
        // Write false if it is null; write true otherwise
        if( XCacheHelper.saveIfNull(dos, obj) ) return;

        // Write the data
        dos.writeUTF(obj);
    }

    public static final void saveArrayListString(final DataOutputStream dos, final ArrayList<String> obj) throws IOException
    {
        // Write the number of items
        dos.writeInt( obj.size() );

        // Write the data
        for(final String item : obj) saveString(dos, item);
    }

    public static void saveASNSpec(final DataOutputStream dos, final XCom.ASNSpec obj) throws IOException
    { dos.writeByte(obj.numCode); }

    public static void saveCompareType(final DataOutputStream dos, final XCom.CompareType obj) throws IOException
    { dos.writeUTF( obj.name() ); }

    public static void saveFuncSpec(final DataOutputStream dos, final XCom.FuncSpec obj) throws IOException
    {
        // Determine the prefix
        final String prefix = obj.supErr ? "-$" : "$";

        // Write the data
        dos.writeUTF( prefix + obj.fnName.name() );
    }

    public static void saveSVarSpec(final DataOutputStream dos, final XCom.SVarSpec obj) throws IOException
    {
        // Write false if it is null; write true otherwise
        if( XCacheHelper.saveIfNull(dos, obj) ) return;

        // Write the data
        switch(obj.svarName) {
            case preqN : dos.writeUTF( obj.svarName.svName(obj.autoIndex) ); break;
            case preqV : dos.writeUTF( obj.svarName.svName(obj.constVal ) ); break;
            default    : dos.writeUTF( obj.svarName.svName(             ) ); break;
        }
    }

    public static void saveALOperName(final DataOutputStream dos, final XCom.ALOperName obj) throws IOException
    {
        // Write the data
        dos.writeUTF( obj.name() );
    }

    public static void saveALOperSpec(final DataOutputStream dos, final XCom.ALOperSpec obj) throws IOException
    {
        // Write the data
        dos.writeUTF( obj.operName.name() );
    }

    public static void saveReadVarSpec(final DataOutputStream dos, final XCom.ReadVarSpec obj) throws IOException
    {
        // Write false if it is an 'XCom.RVSpec_EmptyString'; write true otherwise
        if( System.identityHashCode(obj) == XCom.IHC_RVSpec_EmptyString ) {
            dos.writeBoolean(false);
            return;
        }
        dos.writeBoolean(true);

        // Check if it is not a compile-time constant
        final boolean nctConst = (obj.svarSpec != null) && !obj.svarSpec.svarName.isCompileTimeConstant();

        // Write the data
        dos.writeBoolean(                        obj.constant    );
        saveString      ( dos, nctConst ? null : obj.value       ); // Do not save the value if the constant is not a compile-time constant
        savePattern     ( dos,                   obj.regexp      );
        saveString      ( dos,                   obj.replacement );
        saveSVarSpec    ( dos,                   obj.svarSpec    );
    }

    public static void saveReadVarSpecs(final DataOutputStream dos, final XCom.ReadVarSpecs obj) throws IOException
    {
        // Write false if it is an 'XCom.RVSpcs_EmptyString'; write true otherwise
        if( System.identityHashCode(obj) == XCom.IHC_RVSpcs_EmptyString ) {
            dos.writeBoolean(false);
            return;
        }
        dos.writeBoolean(true);

        // Write the number of items
        dos.writeInt( obj.size() );

        // Write the data
        for(final XCom.ReadVarSpec item : obj) saveReadVarSpec(dos, item);
    }

    public static void saveLabelMap(final DataOutputStream dos, final XCom.LabelMap obj) throws IOException
    {
        // Write false if it is null or empty; write true otherwise
        if( obj == null || obj.isEmpty() ) {
            dos.writeBoolean(false);
            return;
        }
        dos.writeBoolean(true);

        // Write the number of items
        dos.writeInt( obj.size() );

        // Write the data
        for( final Map.Entry<String, Integer> item : obj.entrySet() ) {
            dos.writeUTF( item.getKey  () );
            dos.writeInt( item.getValue() );
        }
    }

    public static void saveVariableStore(final DataOutputStream dos, final XCom.VariableStore obj) throws IOException
    {
        // Write false if it is an 'XCom.VarStr_EmptyString'; write true otherwise
        if( System.identityHashCode(obj) == XCom.IHC_VarStr_EmptyString ) {
            dos.writeBoolean(false);
            return;
        }
        dos.writeBoolean(true);

        // Write the data
        dos.writeBoolean(     obj.constant   );
        saveString      (dos, obj.value      );
        savePattern     (dos, obj.regexp     );
        saveString      (dos, obj.replacement);
    }

    public static void saveVariableValue(final DataOutputStream dos, final XCom.VariableValue obj) throws IOException
    {
        // Write false if it is an 'XCom.VarVal_EmptyString'; write true otherwise
        if( System.identityHashCode(obj) == XCom.IHC_VarVal_EmptyString ) {
            dos.writeBoolean(false);
            return;
        }
        dos.writeBoolean(true);

        // Save the '_const' flag
        dos.writeBoolean( obj.isConstantVariable() );

        // Write the number of items
        dos.writeInt( obj.size() );

        // Write the data
        for(final XCom.VariableStore item : obj) saveVariableStore(dos, item);
    }

    public static void savePostfixTerm(final DataOutputStream dos, final XCom.PostfixTerm obj) throws IOException
    {
        // Write the data
        if( !XCacheHelper.saveIfNull(dos, obj.operand) ) {
            XSaver.saveReadVarSpec(dos, obj.operand);
        }
        else {
            dos.writeUTF( obj.operator.name() );
        }
    }

    public static void savePostfixTerms(final DataOutputStream dos, final XCom.PostfixTerms obj) throws IOException
    {
        // Write the number of items
        dos.writeInt( obj.size() );

        // Write the data
        for(final XCom.PostfixTerm item : obj) savePostfixTerm(dos, item);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void saveTRTokens(final DataOutputStream dos, final ArrayList<TokenReader.Token> trTokens) throws IOException
    {
        dos.writeInt( trTokens.size() );

        for(final TokenReader.Token token : trTokens) token.saveToStream(dos);
    }

    public static void saveMacroDefs(final DataOutputStream dos, final SpecReader.MacroDefs macroDefs) throws IOException
    {
        if( XCacheHelper.saveIfNull(dos, macroDefs ) ) return;

        dos.writeInt( macroDefs.size() );

        for( final Map.Entry< String, ArrayList<TokenReader.Token> > entry : macroDefs.entrySet() ) {
            dos.writeUTF(      entry.getKey  () );
            saveTRTokens( dos, entry.getValue() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean _saveDepListToFile(final String absPath, final ArrayList<String> allSpecFileList)
    {
        // Try to save the dependency list
        try {
            // Open the file
            final BufferedWriter bfw = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(absPath), SysUtil._CharEncoding ) );
            // Write the data
            for(final String line : allSpecFileList) {
                bfw.write(line);
                bfw.newLine();
            }
            // Flush and close the file
            bfw.flush();
            bfw.close();
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Remove the file
            try {
                SysUtil.cu_rmfile(absPath);
            }
            catch(final IOException ex) {}
            // Return error
            return false;
        }

        // Done
        return true;
    }

    private static void _saveCSpecToFile(final DataOutputStream dos, final XCom.ExecBlocks rootExecBlocks) throws IOException
    {
        // Save the header
        XCacheHelper.saveCSpecFileHeader(dos);

        // Save the execution blocks
        XCacheHelper.saveExecBlocks(dos, rootExecBlocks);
    }

    private static boolean _saveToBinFile(final String cspecBinFileAbsPath, final XCom.ExecBlocks rootExecBlocks, final SpecReader.MacroDefs macroDefs, final boolean forCache)
    {
        // Save the compiled specification
        try(
            // Open the file
            final DataOutputStream dos = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(cspecBinFileAbsPath) ) );
        ) {
            // Save the data
            _saveCSpecToFile(dos, rootExecBlocks);
            // Save the macro definitions
            saveMacroDefs(dos, macroDefs);
            // Flush and close the file
            dos.flush();
            // There is no need to call 'dos.close()' because it is a auto-closeable resource
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Remove the file as needed
            if(!forCache) {
                try {
                    SysUtil.cu_rmfile(cspecBinFileAbsPath);
                }
                catch(final IOException ex) {}
            }
            // Print message as needed
            if( !forCache || XCacheHelper.getVerboseMode() ) {
                final String st = SysUtil.stringFromStackTrace(e);
                if(forCache) {
                    SysUtil.printf_stdErr("%s\n", st);
                    SysUtil.printf_stdErr(Texts.IMsg_XSaveJxMakeCacheSaveFail);
                }
                else {
                    SysUtil.printf_stdOut("%s\n", st);
                }
            }
            // Return error
            return false;
        }

        if( forCache && XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XSaveJxMakeCacheSaveDone);

        // Done
        return true;
    }

    public static boolean saveToCacheFile(final String mainJXMSpecFile_absPath, final ArrayList<String> allSpecFileList, final XCom.ExecBlocks rootExecBlocks)
    {
        // Get the compiled specification cache file path
        final String cspecFilePath = XCacheHelper.getCSpecFilePath(mainJXMSpecFile_absPath);

        if(cspecFilePath == null) {
            if( XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XSaveJxMakeNoSpecCachePath);
            return false;
        }

        // Get the compiled specification cache file dependency list file path
        final String cspecFilePath_depList = SysUtil.getCacheFileName_compiledSpecFile_dependencyList(cspecFilePath);

        // Save the dependency list
        if( !_saveDepListToFile(cspecFilePath_depList, allSpecFileList) ) {
            if( XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XSaveJxMakeSpecCacheDFSaveFail);
            return false;
        }

        if( XCacheHelper.getVerboseMode() ) SysUtil.printf_stdErr(Texts.IMsg_XSaveJxMakeSpecCacheDFSaveDone);

        // Save the compiled specification binary data
        if( !_saveToBinFile(cspecFilePath, rootExecBlocks, null, true) ) {
            // Remove the files on error
            try {
                SysUtil.cu_rmfile(cspecFilePath        );
                SysUtil.cu_rmfile(cspecFilePath_depList);
            }
            catch(final IOException ex) {
                // Return error
                return false;
            }
        }

        // Done
        return true;
    }

    public static boolean deleteCacheFile(final String mainJXMSpecFile_absPath, boolean printMsg)
    {
        // Get the compiled specification cache file path
        final String cspecFilePath = XCacheHelper.getCSpecFilePath(mainJXMSpecFile_absPath);
        if(cspecFilePath == null) return false;

        // Get the compiled specification cache file dependency list file path
        final String cspecFilePath_depList = SysUtil.getCacheFileName_compiledSpecFile_dependencyList(cspecFilePath);

        // Print message as needed
        if(printMsg) {
            SysUtil.printf_stdErr(Texts.IMsg_DeletingCompiledJxMakeSpecCache, cspecFilePath        );
            SysUtil.printf_stdErr(Texts.IMsg_DeletingCompiledJxMakeSpecCache, cspecFilePath_depList);
        }

        // Remove the files
        try {
            SysUtil.cu_rmfile(cspecFilePath        );
            SysUtil.cu_rmfile(cspecFilePath_depList);
        }
        catch(final IOException ex) {
            // Return error
            return false;
        }

        // Done
        return true;
    }

    public static boolean saveToLibraryFile(final String cspecBinFileAbsPath, final XCom.ExecBlocks rootExecBlocks, final SpecReader.MacroDefs macroDefs)
    { return _saveToBinFile(cspecBinFileAbsPath, rootExecBlocks, macroDefs, false); }

} // class XSaver
