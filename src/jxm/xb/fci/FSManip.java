/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Pattern;

import jxm.*;
import jxm.xb.*;


public class FSManip {

    public static void _execute_touch(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "touch", "<path>");
                SysUtil.cu_touch( SysUtil.resolveAbsolutePath(path) );
            }
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_rmfile(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "rmfile", "<path>");
                SysUtil.cu_rmfile( SysUtil.resolveAbsolutePath(path) );
            }
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_rmfiles_rnr(final ArrayList<XCom.VariableValue> evalVals, final boolean recursive) throws JXMException
    {
        // Get the arguments and the number of items
        final XCom.VariableValue subject = evalVals.get(0);
        final XCom.VariableValue pattern = evalVals.get(1);
        final int                cnt     = Math.max( subject.size(), pattern.size() );

        try {
            // Perform regular expression file removal
            for(int i = 0; i < cnt; ++i) {
                // Get the arguments
                final String strPath   = ( ( i < subject.size() ) ? subject.get(i).value : subject.get( subject.size() - 1 ).value ).trim();
                final String strRegExp =   ( i < pattern.size() ) ? pattern.get(i).value : pattern.get( pattern.size() - 1 ).value;
                if( strPath  .isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr  , "rmfiles[_rec]", "<path_dir>"    );
                if( strRegExp.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyRegExpStr, "rmfiles[_rec]", "<regexp_value>");
                // Remove files
                final Pattern           patRegExp = ReCache._reGetPattern(strRegExp);
                final ArrayList<String> resList   = recursive ? SysUtil.cu_find_file_recursive(strPath, patRegExp)
                                                              : SysUtil.cu_find_file          (strPath, patRegExp);
                for(final String item : resList) {
                    SysUtil.cu_rmfile( SysUtil.resolveAbsolutePath(item) );
                }
            }
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_cpmvfile(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean mv) throws JXMException
    {
        try {

            // Get the file names
            final ArrayList<String> srcNames = new ArrayList<>();
            final ArrayList<String> dstNames = new ArrayList<>();

            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, mv ? "mvfile" : "cpfile", "<path_src>");
                srcNames.add( SysUtil.resolveAbsolutePath(path) );
            }

            for( final XCom.VariableStore item : evalVals.get(1) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, mv ? "mvfile" : "cpfile", "<path_dst>");
                dstNames.add( SysUtil.resolveAbsolutePath(path) );
            }

            // Get the flag
            final boolean replaceExisting =        XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "false") );
            final boolean normalizePFP    = !mv && XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 3, "false") );

            // If the number of source and destination files are not the same, raise an error
            final int srcCount = srcNames.size();
            final int dstCount = dstNames.size();

            if(srcCount != dstCount) throw XCom.newJXMRuntimeError(Texts.EMsg_xxfile_NumFileNotSame, mv ? "mv" : "cp", srcCount, dstCount);

            // Move the files
            for(int i = 0; i < srcCount; ++i) {
                if(mv) SysUtil.cu_mvfile( srcNames.get(i), dstNames.get(i), replaceExisting               );
                else   SysUtil.cu_cpfile( srcNames.get(i), dstNames.get(i), replaceExisting, normalizePFP );
            }

        } // try
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_mkdir(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "mkdir", "<path_dir>");
                SysUtil.cu_mkdir( SysUtil.resolveAbsolutePath(path) );
            }
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_rmdir(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "rmdir", "<path_dir>");
                SysUtil.cu_rmdir( SysUtil.resolveAbsolutePath(path) );
            }
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_rmdir_rec(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "rmdir_rec", "<path_dir>");
                SysUtil.cu_rmdir_recursive( SysUtil.resolveAbsolutePath(path) );
            }
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_cpdir_rec(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        try {

            // Get the paths and flags
            final ArrayList<String > srcPaths = new ArrayList<>();
            final ArrayList<String > dstPaths = new ArrayList<>();
            final ArrayList<Boolean> repExist = new ArrayList<>();

            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "cpdir_rec", "<path_src>");
                srcPaths.add( SysUtil.resolveAbsolutePath(path) );
            }

            for( final XCom.VariableStore item : evalVals.get(1) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "cpdir_rec", "<path_dst>");
                dstPaths.add( SysUtil.resolveAbsolutePath(path) );
            }

            for( final XCom.VariableStore item : evalVals.get(2) ) {
                repExist.add( XCom.toBoolean(execBlock, execData, item.value) );
            }

            // If the number of source and destination paths are not the same, raise an error
            final int srcCount = srcPaths.size();
            final int dstCount = dstPaths.size();
            final int repCount = repExist.size();

            if( !(srcCount == repCount && dstCount == repCount) ) throw XCom.newJXMRuntimeError(Texts.EMsg_cpdir_rec_NumDirNotSame, srcCount, dstCount, repCount);

            // Recursively copy the directories
            for(int i = 0; i < srcCount; ++i) {
                SysUtil.cu_cpdir_rec( srcPaths.get(i), dstPaths.get(i), repExist.get(i) );
            }

        } // try
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_lsdir_rnr(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean recursive) throws JXMException
    {
        // Get the optional 'type' argument
        final ArrayList<Character> type    = new ArrayList<>();
        final XCom.VariableValue   typeVal = FuncCall._getOptParam(evalVals, 1);
        if(typeVal != null) {
            for( final XCom.VariableStore item : typeVal ) {
                final String ts = item.value.trim();
                final char   tc = ( ts.length() == 1 ) ? ts.charAt(0) : SysUtil.LSFD_Type_All;
                type.add( (tc == SysUtil.LSFD_Type_File || tc == SysUtil.LSFD_Type_Directory) ? tc : SysUtil.LSFD_Type_All );
            }
        }
        if( type.isEmpty() ) type.add( Character.valueOf(SysUtil.LSFD_Type_All) );

        // Get the optional 'max_depth' argument
        final ArrayList<Integer> depthMax = new ArrayList<>();
        if(recursive) {
            final XCom.VariableValue depthMaxVal = FuncCall._getOptParam(evalVals, 2);
            if(depthMaxVal != null) {
                for( final XCom.VariableStore item : depthMaxVal ) depthMax.add( XCom.toLong(execBlock, execData, item.value).intValue() );
            }
        }
        if( depthMax.isEmpty() ) depthMax.add( Integer.valueOf(-1) );

        try {
            for(int i = 0; i < evalVals.get(0).size(); ++i) {
                // Get the arguments
                final String path      = evalVals.get(0).get(i).value.trim();
                final char   typeC     = ( i < type    .size() ) ? type    .get(i) : type    .get( type    .size() - 1 );
                final int    depthMaxI = ( i < depthMax.size() ) ? depthMax.get(i) : depthMax.get( depthMax.size() - 1 );
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "lsdir[_rec]", "<path_dir>");
                // List directory
                final List<String> res = recursive ? SysUtil.cu_lsfd_rec( SysUtil.resolveAbsolutePath(path), typeC, depthMaxI )
                                                   : SysUtil.cu_lsfd    ( SysUtil.resolveAbsolutePath(path), typeC            );
                for(final String r : res) {
                    retVal.add( new XCom.VariableStore(true, r) );
                }
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

    public static void _execute_srfd_rec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean searchForDir) throws JXMException
    {
        // Get the arguments and the number of items
        final XCom.VariableValue subject     = evalVals.get(0);
        final XCom.VariableValue pattern     = evalVals.get(1);
        final XCom.VariableValue depthMaxVal = FuncCall._getOptParam(evalVals, 2);
        final int                cnt         = Math.max( subject.size(), pattern.size() );

        final ArrayList<Integer> depthMax = new ArrayList<>();
        if(depthMaxVal != null) {
            for( final XCom.VariableStore item : depthMaxVal ) depthMax.add( XCom.toLong(execBlock, execData, item.value).intValue() );
        }
        if( depthMax.isEmpty() ) depthMax.add( Integer.valueOf(-1) );

        try {
            // Perform regular expression searching
            for(int i = 0; i < cnt; ++i) {
                // Get the arguments
                final String strPath   = ( ( i < subject. size() ) ? subject .get(i).value : subject .get( subject .size() - 1 ).value ).trim();
                final String strRegExp =   ( i < pattern .size() ) ? pattern .get(i).value : pattern .get( pattern .size() - 1 ).value;
                final int    depthMaxI =   ( i < depthMax.size() ) ? depthMax.get(i)       : depthMax.get( depthMax.size() - 1 );
                if( strPath  .isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr  , searchForDir ? "srdir_rec" : "srfile_rec", "<path_dir>"    );
                if( strRegExp.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyRegExpStr, searchForDir ? "srdir_rec" : "srfile_rec", "<regexp_value>");
                // Perform search
                final Pattern           patRegExp = ReCache._reGetPattern(strRegExp);
                final ArrayList<String> resList   = searchForDir ? SysUtil.cu_find_dir_recursive (strPath, patRegExp, depthMaxI)
                                                                 : SysUtil.cu_find_file_recursive(strPath, patRegExp, depthMaxI);
                for(final String item : resList) {
                    retVal.add( new XCom.VariableStore(true, item) );
                }
            }
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

} // class FSManip
