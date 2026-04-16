/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;

import jxm.*;
import jxm.tool.*;
import jxm.xb.*;


public class FCUtil {

    private interface FCompRec
    { void apply(final String arcvFilePath, final String srcDirPath, final String appendParentDirName) throws IOException; }

    private interface FUncompRec
    { void apply(final String arcvFilePath, final String dstDirPath) throws IOException; }

    private interface FCompUncomp
    { void apply(final String inpFilePath, final String outFilePath) throws IOException; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _execute_impl_comp_uncomp_rec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final String bifName, final String arcvPathName, final FCompRec funCompImpl, final FUncompRec funUncompImpl) throws JXMException
    {
        final boolean comp = (funCompImpl != null);
        final String  arcv = XCom.flatten( evalVals.get(0), "" ).trim();
        final String  dirp = XCom.flatten( evalVals.get(1), "" ).trim();
        final String  apdn = FuncCall._readFlattenOptParam(evalVals, 2, null);
        final boolean xerr = XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, comp ? 3 : 2, "true") );

        if( arcv.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, bifName, arcvPathName                      );
        if( dirp.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, bifName, comp ? "<src_path>" : "<dst_path>");

        try {
            if(comp) funCompImpl  .apply(arcv, dirp, apdn);
            else     funUncompImpl.apply(arcv, dirp      );
        }
        catch(final IOException e) {
            // Store the error message and return if the exit-on-error flag is not set
            if(!xerr) {
                retVal.add( new XCom.VariableStore(
                                true,
                                comp ? String.format( "%s\n$%s('%s', '%s', '%s', false)", e.toString(), bifName, arcv, dirp, apdn )
                                     : String.format( "%s\n$%s('%s', '%s', false)"      , e.toString(), bifName, arcv, dirp       )
                          ) );
                return;
            }
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_txzdir_rec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean uncompress) throws JXMException
    {
        if(uncompress) _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "untxz_rec"  , "<tarxz_file_name>" , null               , TarXz ::uncompressDir);
        else           _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "txzdir_rec" , "<tarxz_file_name>" , TarXz ::compressDir, null                 );
    }

    public static void _execute_tbz2dir_rec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean uncompress) throws JXMException
    {
        if(uncompress) _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "untbz2_rec" , "<tarbz2_file_name>", null               , TarBz2::uncompressDir);
        else           _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "tbz2dir_rec", "<tarbz2_file_name>", TarBz2::compressDir, null                 );
    }

    public static void _execute_tgzdir_rec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean uncompress) throws JXMException
    {
        if(uncompress) _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "untgz_rec"  , "<targz_file_name>" , null               , TarGz ::uncompressDir);
        else           _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "tgzdir_rec" , "<targz_file_name>" , TarGz ::compressDir, null                 );
    }

    public static void _execute_tzipdir_rec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean uncompress) throws JXMException
    {
        if(uncompress) _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "untzip_rec" , "<tarzip_file_name>", null               , TarZip::uncompressDir);
        else           _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "tzipdir_rec", "<tarzip_file_name>", TarZip::compressDir, null                 );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_untardir_rec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {  _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "untar_rec", "<tar_file_name>", null, TarPlain::uncompressDir); }

    public static void _execute_unzipdir_rec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {  _execute_impl_comp_uncomp_rec(retVal, evalVals, execBlock, execData, "unzip_rec", "<zip_file_name>", null, UnZip::uncompressDir   ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _execute_impl_comp_uncomp(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final String bifName, final FCompUncomp funImpl) throws JXMException
    {
        // Get the exit-on-error flag
        final boolean xerr = XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "true") );

        // Working variables
        final ArrayList<String> inpFiles = new ArrayList<>();
        final ArrayList<String> outFiles = new ArrayList<>();
              int               idxCnt   = 0;

        try {

            // Get the file paths
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, bifName, "<input_file_name>");
                inpFiles.add( SysUtil.resolveAbsolutePath(path) );
            }

            for( final XCom.VariableStore item : evalVals.get(1) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, bifName, "<output_file_name>");
                outFiles.add( SysUtil.resolveAbsolutePath(path) );
            }

            // If the number of source and destination files are not the same, raise an error
            final int inpCount = inpFiles.size();
            final int outCount = outFiles.size();

            if(inpCount != outCount) throw XCom.newJXMRuntimeError(Texts.EMsg_gzip_NumFileNotSame, bifName, inpCount, outCount);

            // Process the files
            for(; idxCnt < inpCount; ++idxCnt) {
                funImpl.apply( inpFiles.get(idxCnt), outFiles.get(idxCnt) );
            }

        } // try
        catch(final Exception e) {
            // Store the error message and return if the exit-on-error flag is not set
            if(!xerr) {
                retVal.add( new XCom.VariableStore(
                                true,
                                String.format( "%s\n$%s('%s', '%s', false)", e.toString(), bifName, inpFiles.get(idxCnt), outFiles.get(idxCnt) )
                          ) );
                return;
            }
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_xz(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean uncompress) throws JXMException
    {
        if(uncompress) _execute_impl_comp_uncomp(retVal, evalVals, execBlock, execData, "unxz"   , TarXz ::unxz  );
        else           _execute_impl_comp_uncomp(retVal, evalVals, execBlock, execData, "xz"     , TarXz ::xz    );
    }

    public static void _execute_bzip2(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean uncompress) throws JXMException
    {
        if(uncompress) _execute_impl_comp_uncomp(retVal, evalVals, execBlock, execData, "bunzip2", TarBz2::bunzip2);
        else           _execute_impl_comp_uncomp(retVal, evalVals, execBlock, execData, "bzip2"  , TarBz2::bzip2  );
    }

    public static void _execute_gzip(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean uncompress) throws JXMException
    {
        if(uncompress) _execute_impl_comp_uncomp(retVal, evalVals, execBlock, execData, "gunzip" , TarGz ::gunzip );
        else           _execute_impl_comp_uncomp(retVal, evalVals, execBlock, execData, "gzip"   , TarGz ::gzip   );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_put_file(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Check if there are multiple destination files
        if( evalVals.get(0).size() != 1 ) throw XCom.newJXMRuntimeError(Texts.EMsg_WriteMultipleDstFile, "put_file");

        // Get the file path
        String dstFile = evalVals.get(0).get(0).value.trim();

        if( dstFile.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "put_file", "<path>");

        // Modify the path as needed
        final String tmpDir = SysUtil.getSavedProjectTmpDir();

        if(tmpDir == null) throw XCom.newJXMRuntimeError(Texts.EMsg_NoProjectTmpDir);

        dstFile = SysUtil.resolvePath(dstFile, tmpDir);

        // Write the data
        try {
            // Open the file
            final BufferedWriter bfw = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(dstFile), SysUtil._CharEncoding ) );
            // Write the data
            for( final XCom.VariableStore item : evalVals.get(1) ) {
                bfw.write(item.value);
                bfw.newLine();
            }
            // Flush and close the file
            bfw.flush();
            bfw.close();
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }

        // Return the modified path
        retVal.add( new XCom.VariableStore(true, dstFile) );
    }

    public static void _execute_get_file(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        final boolean       wholeText = XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "false") );
        final StringBuilder sb        = wholeText ? new StringBuilder() : null;

        try {
            // Process the file(s)
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                // Get the file path
                final String path = item.value.trim();
                if( path.isEmpty() ) continue;
                // Open the file
                final BufferedReader bfr = new BufferedReader( new InputStreamReader( new FileInputStream( SysUtil.resolveAbsolutePath(path) ), SysUtil._CharEncoding ) );
                // Read the data
                while(true) {
                    // Read one line
                    final String line = bfr.readLine();
                    if(line == null) break;
                    // Combine the line
                    if(wholeText) {
                        sb.append(line);
                        sb.append('\n');
                    }
                    // Store the line
                    else {
                        retVal.add( new XCom.VariableStore(true, line) );
                    }
                }
                // Close the file
                bfr.close();
            }
            // Store combined text
            if(wholeText) retVal.add( new XCom.VariableStore( true, sb.toString() ) );
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_get_file_nel(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        int begLine = XCom.toLong( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") ).intValue();
        int endLine = XCom.toLong( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "0") ).intValue();

        if(begLine < 1) begLine = 1;
        if(endLine < 1) endLine = 2147483647;

        try {
            // Process the file(s)
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                // Get the file path
                final String path = item.value.trim();
                if( path.isEmpty() ) continue;
                // Open the file
                final BufferedReader bfr = new BufferedReader( new InputStreamReader( new FileInputStream( SysUtil.resolveAbsolutePath(path) ), SysUtil._CharEncoding ) );
                      int            idx = 0;
                // Read the data
                while(true) {
                    // Read one line
                    final String line = bfr.readLine();
                    if(line == null) break;
                    // Skip effectively empty lines
                    if( line.trim().isEmpty() ) continue;
                    // Skip if the counter is still smaller than the begin line index
                    if(++idx < begLine) continue;
                    // Store the line as
                    retVal.add( new XCom.VariableStore(true, line) );
                    // Break if the counter has reached the end line index
                    if(idx >= endLine) break;
                }
                // Close the file
                bfr.close();
            }
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_md_file(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final String algorithm) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                // Get the file path
                final String path = item.value.trim();
                if( path.isEmpty() ) continue;
                // Compute the message digest
                final String mdStr = SysUtil.computeFileHash( SysUtil.resolveAbsolutePath(path), algorithm );
                // Store the result
                retVal.add( new XCom.VariableStore(true, mdStr) );
            }
        }
        catch(final NoSuchAlgorithmException e) {
            // Tell the user that the requested algorithm does not exist
            retVal.add( new XCom.VariableStore(true, "NoSuchAlgorithmException") );
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

} // class FCUtil
