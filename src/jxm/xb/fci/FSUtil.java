/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.util.ArrayList;

import jxm.*;
import jxm.xb.*;


public class FSUtil {

    public static void _execute_cwd(final XCom.VariableValue retVal) throws JXMException
    { retVal.add( new XCom.VariableStore( true, SysUtil.getCWD() ) ); }

    public static void _execute_ptd(final XCom.VariableValue retVal) throws JXMException
    {
        final String dir = SysUtil.getSavedProjectTmpDir();

        if(dir == null) throw XCom.newJXMRuntimeError(Texts.EMsg_NoProjectTmpDir);

        retVal.add( new XCom.VariableStore(true, dir) );
    }

    public static void _execute_uhd(final XCom.VariableValue retVal) throws JXMException
    { retVal.add( new XCom.VariableStore( true, SysUtil.getUHD() ) ); }

    public static void _execute_jdd(final XCom.VariableValue retVal) throws JXMException
    {
        final String dir = SysUtil.getJDD();

        if(dir == null) throw XCom.newJXMRuntimeError(Texts.EMsg_NoJxMakeUDataDir);

        retVal.add( new XCom.VariableStore(true, dir) );
    }

    public static void _execute_jtd(final XCom.VariableValue retVal) throws JXMException
    {
        final String dir = SysUtil.getJTD();

        if(dir == null) throw XCom.newJXMRuntimeError(Texts.EMsg_NoJxMakeUToolsDir);

        retVal.add( new XCom.VariableStore(true, dir) );
    }

    public static void _execute_jxd(final XCom.VariableValue retVal) throws JXMException
    {
        final String dir = SysUtil.getJXD();

        if(dir == null) throw XCom.newJXMRuntimeError(Texts.EMsg_NoJxMakeExeDir);

        retVal.add( new XCom.VariableStore(true, dir) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_set_rwx3(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the number of entries
        final int pCount = evalVals.get(0).size();
        final int sCount = evalVals.get(1).size();
        final int mCount = Math.max(pCount, sCount);

        if(pCount == 0 || sCount == 0) return;

        // Get the last entries
        final String pLast = evalVals.get(0).get(pCount - 1).value;
        final String sLast = evalVals.get(1).get(sCount - 1).value;

        // Set the permission(s)
        try {
            for(int i = 0; i < mCount; ++i) {
                final String path = ( (i < pCount) ? evalVals.get(0).get(i).value : pLast ).trim();
                final String srwx = ( (i < sCount) ? evalVals.get(1).get(i).value : sLast ).trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "set_rwx3", "<path>"     );
                if( srwx.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "set_rwx3", "<rwxrwxrwx>");
                SysUtil.cu_set_rwx3( SysUtil.resolveAbsolutePath(path), srwx );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_cat_path(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the number of paths
        final int p1Count = evalVals.get(0).size();
        final int p2Count = evalVals.get(1).size();
        final int pmCount = Math.max(p1Count, p2Count);

        if(p1Count == 0 || p2Count == 0) return;

        // Get the last paths
        final String p1Last = evalVals.get(0).get(p1Count - 1).value;
        final String p2Last = evalVals.get(1).get(p2Count - 1).value;

        // Concatenate the path(s)
        for(int i = 0; i < pmCount; ++i) {
            final String path    = ( (i < p2Count) ? evalVals.get(1).get(i).value : p2Last ).trim();
            final String pathRef = ( (i < p1Count) ? evalVals.get(0).get(i).value : p1Last ).trim();
            if( path   .isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "cat_path", "<path2>");
            if( pathRef.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "cat_path", "<path1>");
            retVal.add( new XCom.VariableStore( true, SysUtil.resolvePath(path, pathRef) ) );
        }
    }

    public static void _execute_cat_paths(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the number of paths
        int pmCount = 0;

        for(final XCom.VariableValue varVal : evalVals) {
            final int cpCount = varVal.size();
            if(cpCount == 0) return;
            if(cpCount > pmCount) pmCount = cpCount;
        }

        // Concatenate the path(s)
        for(int i = 0; i < pmCount; ++i) {
            String pathRef = null;
            for(final XCom.VariableValue varVal : evalVals) {
                // Get the path
                final int    cpCount = varVal.size();
                final String path    = ( (i < cpCount) ? varVal.get(i).value : varVal.get(cpCount - 1).value ).trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "cat_paths", "<path" +  String.valueOf(i) + ">");
                // If this is the first one store it as the reference path
                if(pathRef == null) {
                    pathRef = path;
                    continue;
                }
                // Concatenate the paths and update the reference path
                else {
                    final String resPath = SysUtil.resolvePath(path, pathRef);
                    pathRef = resPath;
                }
            } // for
            // Store the result
            retVal.add( new XCom.VariableStore(true, pathRef) );
        } // for
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_abs_path(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "abs_path", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.resolveAbsolutePath(path) ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_rel_path(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "rel_path", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.resolveRelativePath( SysUtil.resolveAbsolutePath(path) ) ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_valid_path(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                retVal.add( new XCom.VariableStore(
                    true,
                    path.isEmpty() ? XCom.Str_F
                                   : SysUtil.pathIsValid( SysUtil.resolveAbsolutePath(path) ) ? XCom.Str_T : XCom.Str_F
                ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_uptodate_path(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final XCom.ExecData execData) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "uptodate_path", "<path>");
                final String absPath = SysUtil.resolveAbsolutePath(path);
                if( !SysUtil.pathIsValid(absPath) ) {
                    retVal.add( new XCom.VariableStore(true, XCom.Str_F) );
                }
                else {
                    final boolean uptodate = ( SysUtil.pathGetTime(absPath).compareTo(execData.latestFileTime) > 0 );
                    retVal.add( new XCom.VariableStore(true, uptodate ? XCom.Str_T : XCom.Str_F) );
                }
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_newer_path(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {

            // Get the file names
            final ArrayList<String> chkNames = new ArrayList<>();
            final ArrayList<String> refNames = new ArrayList<>();

            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "newer_path", "<path_chk>");
                chkNames.add( SysUtil.resolveAbsolutePath(path) );
            }

            for( final XCom.VariableStore item : evalVals.get(1) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "newer_path", "<path_ref>");
                refNames.add( SysUtil.resolveAbsolutePath(path) );
            }

            final int chkSize = chkNames.size();
            final int refSize = refNames.size();

            // If the number of files to be checked is greater than the number of reference files, return false immediately
            if(chkSize > refSize) {
                // Return one entry
                retVal.add( new XCom.VariableStore(true, XCom.Str_F) );
                return;
            }

            // If the number of files to be checked is only one
            if(chkSize == 1) {
                // Return one entry
                retVal.add( new XCom.VariableStore( true, SysUtil.isPathMoreRecent( chkNames.get(0), refNames ) ? XCom.Str_T : XCom.Str_F ) );
                return;
            }

            // If the number of files to be checked is less than the number of reference files, duplicate the last name
            if(chkSize < refSize) {
                final String lname = chkNames.get(chkSize - 1);
                for(int i = 0; i < refSize - chkSize; ++i) chkNames.add(lname);
            }

            // Perform comparison
            for(int i = 0; i < refSize; ++i) {
                // Return multiple entries
                retVal.add( new XCom.VariableStore( true, SysUtil.isPathMoreRecent( chkNames.get(i), refNames.get(i) ) ? XCom.Str_T : XCom.Str_F ) );
            }

        } // try
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_same_path(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {

            // Get the file names
            final ArrayList<String> chkNames = new ArrayList<>();
            final ArrayList<String> refNames = new ArrayList<>();

            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "same_path", "<path_chk>");
                chkNames.add( SysUtil.resolveAbsolutePath(path) );
            }

            for( final XCom.VariableStore item : evalVals.get(1) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "same_path", "<path_ref>");
                refNames.add( SysUtil.resolveAbsolutePath(path) );
            }

            final int chkSize = chkNames.size();
            final int refSize = refNames.size();

            // If the number of files to be checked is greater than the number of reference files, return false immediately
            if(chkSize > refSize) {
                // Return one entry
                retVal.add( new XCom.VariableStore(true, XCom.Str_F) );
                return;
            }

            // If the number of files to be checked is only one
            if(chkSize == 1) {
                for(final String refName : refNames) {
                    retVal.add( new XCom.VariableStore( true, SysUtil.pathIsSame( chkNames.get(0), refName ) ? XCom.Str_T : XCom.Str_F ) );
                }
                return;
            }

            // If the number of files to be checked is less than the number of reference files, duplicate the last name
            if(chkSize < refSize) {
                final String lname = chkNames.get(chkSize - 1);
                for(int i = 0; i < refSize - chkSize; ++i) chkNames.add(lname);
            }

            // Perform comparison
            for(int i = 0; i < refSize; ++i) {
                // Return multiple entries
                retVal.add( new XCom.VariableStore( true, SysUtil.pathIsSame( chkNames.get(i), refNames.get(i) ) ? XCom.Str_T : XCom.Str_F ) );
            }

        } // try
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_path_is_abs(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "path_is_abs", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathIsAbsolute(path) ? XCom.Str_T : XCom.Str_F ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_path_is_rel(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "path_is_rel", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathIsRelative(path) ? XCom.Str_T : XCom.Str_F ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_path_is_file(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "path_is_file", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathIsFile(path) ? XCom.Str_T : XCom.Str_F ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_path_is_dir(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "path_is_directory", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathIsDirectory(path) ? XCom.Str_T : XCom.Str_F ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_path_is_syml(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "$path_is_symlink", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathIsSymLink(path) ? XCom.Str_T : XCom.Str_F ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_path_is_rable(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "$path_is_readable", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathIsReadable(path) ? XCom.Str_T : XCom.Str_F ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_path_is_wable(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "$path_is_writable", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathIsWritable(path) ? XCom.Str_T : XCom.Str_F ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_path_is_xable(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "$path_is_executable", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathIsExecutable(path) ? XCom.Str_T : XCom.Str_F ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_path_lpart(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {

            // Get and check the path
            String path = item.value.trim();
            if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "path_last_part", "<path>");

            // Normalize the directory separator(s)
            path = SysUtil.normalizeDirectorySeparators(path);

            // Check if the last character is '_InternalDirSep'
            if( path.charAt( path.length() - 1 ) == SysUtil._InternalDirSep ) {
                // Remove the last character
                path = path.substring( 0, path.length() - 1 );
                // If the path becomes empty, simply store an empty string
                if( path.isEmpty() ) {
                    retVal.add( new XCom.VariableStore(true, "") );
                    continue;
                }
            }

            // Get the index to the last '_InternalDirSep'
            final int lpPos = path.lastIndexOf​(SysUtil._InternalDirSep);

            // If there is only one part, simply store back the path
            if(lpPos < 0) {
                retVal.add( new XCom.VariableStore(true, path) );
                continue;
            }

            // Store the last part
            retVal.add( new XCom.VariableStore( true, path.substring( lpPos + 1, path.length() ) ) );

        } // for
    }

    public static void _execute_path_rmlpart(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {

            // Get and check the path
            String path = item.value.trim();
            if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "path_rm_last_part", "<path>");

            // Normalize the directory separator(s)
            path = SysUtil.normalizeDirectorySeparators(path);

            // Check if the last character is '_InternalDirSep'
            if( path.charAt( path.length() - 1 ) == SysUtil._InternalDirSep ) {
                // Remove the last character
                path = path.substring( 0, path.length() - 1 );
                // If the path becomes empty, simply store an empty string
                if( path.isEmpty() ) {
                    retVal.add( new XCom.VariableStore(true, "") );
                    continue;
                }
            }

            // Get the index to the last '_InternalDirSep'
            final int lpPos = path.lastIndexOf​(SysUtil._InternalDirSep);

            // If there is only one part, simply store an empty string
            if(lpPos < 0) {
                retVal.add( new XCom.VariableStore(true, "") );
                continue;
            }

            // Store the last part
            retVal.add( new XCom.VariableStore( true, path.substring(0, lpPos) ) );

        } // for
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_path_ndsep(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            final String path = item.value.trim();
            if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "path_ndsep", "<path>");
            retVal.add( new XCom.VariableStore( true, SysUtil.toNativeDirectorySeparators(path) ) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_syml_target(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "symlink_target", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathGetSymLinkTarget( SysUtil.resolveAbsolutePath(path) ) ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_syml_rapath(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "symlink_real_apath", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathGetSymLinkRealPath_ignoreNonExistentTarget( SysUtil.resolveAbsolutePath(path) ) ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_syml_resolve(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "symlink_resolve", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.pathGetRealPath(path) ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_dir_name(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "dir_name", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.getDirName( SysUtil.resolveAbsolutePath(path) ) ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_file_name(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the optional parameter
        final XCom.VariableValue noExtVV = FuncCall._getOptParam(evalVals, 1);
        final ArrayList<Boolean> noExt   = new ArrayList<>();

        if(noExtVV != null) {
            for( final XCom.VariableStore item : noExtVV ) {
                noExt.add( XCom.toBoolean(execBlock, execData, item.value) );
            }
        }
        else {
            noExt.add(Boolean.FALSE);
        }

        // Get the file name
        try {
            for( int i = 0; i < evalVals.get(0).size(); ++i ) {
                // Get the path
                final String path = evalVals.get(0).get(i).value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "file_name", "<path>");
                // Get the flag
                final boolean ne = ( i < noExt.size() ) ? noExt.get(i) : noExt.get( noExt.size() - 1 );
                // Get the file name
                retVal.add( new XCom.VariableStore(
                    true,
                    ne ? SysUtil.getFileNameWithoutExtension( SysUtil.resolveAbsolutePath(path) )
                       : SysUtil.getFileName                ( SysUtil.resolveAbsolutePath(path) )
                ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_file_ext(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "file_ext", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.getFileExtension( SysUtil.resolveAbsolutePath(path) ) ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_file_mime_typ(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        try {
            for( final XCom.VariableStore item : evalVals.get(0) ) {
                final String path = item.value.trim();
                if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "file_mime_type", "<path>");
                retVal.add( new XCom.VariableStore( true, SysUtil.cu_file_mimetype( SysUtil.resolveAbsolutePath(path) ) ) );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

} // class FSUtil
