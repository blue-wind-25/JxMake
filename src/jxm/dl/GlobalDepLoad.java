/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.dl;


import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jxm.*;
import jxm.xb.*;


public class GlobalDepLoad {

    private static final Pattern                            _pmToken           = Pattern.compile("(?:\\\\\\s|\\S)+");
    private static final TreeMap< String, TreeSet<String> > _globalDepMap      = new TreeMap<>(); // NOTE : Sorted
    private static final TreeSet< String                  > _globalCpp20ModSet = new TreeSet<>(); // NOTE : Sorted

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static void addDepFor(final String name, final String value)
    {
        TreeSet<String> mapEntry = _globalDepMap.get(name);

        if(mapEntry == null) {
            mapEntry = new TreeSet<>();
            _globalDepMap.put(name, mapEntry);
        }

        mapEntry.add(value);
    }

    public synchronized static TreeMap< String, TreeSet<String> > getAllDeps()
    { return _globalDepMap;  }

    public synchronized static TreeSet<String> getDepFor(final String name)
    { return _globalDepMap.get(name);  }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static void dumpDepMap(final TreeMap< String, TreeSet<String> > depMap)
    {
        for( final Map.Entry< String, TreeSet<String> > entry : depMap.entrySet() ) {
            SysUtil.stdOut().println("[[[ " + entry.getKey() + " ]]]");
            for( final String item : entry.getValue() ) SysUtil.stdOut().println(item);
            SysUtil.stdOut().println();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private synchronized static String _checkDependencyRecursion(final int level, final TreeMap< String, TreeSet<String> > depMap, final String chkTargetName, final String curPreqName)
    {
        // Check the recursion depth
        final boolean  hasCPN = (curPreqName != null);

        if(level >= 200) return hasCPN ? curPreqName : chkTargetName;

        // Get the dependency list for the current item
        final TreeSet<String> preqList = depMap.get(hasCPN ? curPreqName : chkTargetName);

        if(preqList == null) return null;

        // Check the names
        for(final String preq : preqList) {
            // Check if the current name is equal to the target name
            if( preq.equals(chkTargetName) ) return hasCPN ? curPreqName : preq;
            // Check if the current name is equal to the current prerequisite name
            if( curPreqName != null && preq.equals(curPreqName) ) return preq;
            // Recursively check deeper
            final String errPreq = _checkDependencyRecursion(level + 1, depMap, chkTargetName, preq);
            if(errPreq != null) return errPreq;
        }

        // Done
        return null;
    }

    private synchronized static TreeMap< String, TreeSet<String> > _loadDepData(final String depFileAbsDir, final BufferedReader bfr, final boolean debug, final boolean forTestOnly) throws IOException
    {
        // NOTE : If the 'forTestOnly' flag is set, some code path will be disabled.

        // Prepare a temporary map
        TreeMap< String, TreeSet<String> > depMap = new TreeMap<>();

        // Prepare the string builder
        // NOTE : Use 'StringBuilder' here because the chance of '\\' appearing would be quite often if the dependency
        //        files are manually written by the user
        StringBuilder sb = new StringBuilder();

        // Flags and state
        boolean smCppInclude  = false;
        boolean smCpp20Module = false;
        boolean smJavaImport  = false;

        String  cppObjExt     = null;

        // Read the data
        while(true) {

            // Read one line
            final String line = bfr.readLine();
            if( line == null && sb.length() == 0 ) break;

            // Store the line
            if( line != null && !line.isEmpty() ) {
                // Check if the last character is the line continuation character
                if( line.charAt( line.length() - 1 ) == '\\' ) {
                    // Concatenate the line (minus the last character)
                    sb.append( line.substring( 0, line.length() - 1 ) );
                    continue;
                }
                // No line continuation character
                else {
                    sb.append(line);
                }
            }

            // Get the combined line
            final String lineStr = sb.toString();
            sb.setLength(0);

            if( lineStr.isEmpty() ) continue;

            // Check the dependency list type
            switch(lineStr) {
                case DepReader._smCppInclude:
                    smCppInclude  = true;
                    smCpp20Module = false;
                    smJavaImport  = false;
                    continue;
                case DepReader._smCpp20Module:
                    smCppInclude  = false;
                    smCpp20Module = true;
                    smJavaImport  = false;
                    continue;
                case DepReader._smJavaImport:
                    smCppInclude  = false;
                    smCpp20Module = false;
                    smJavaImport  = true;
                    continue;
            }

            // Perform regular expression matching
            final Matcher matcher = _pmToken.matcher(lineStr);

            // Get and check the target name
            String targetName = matcher.find() ? matcher.group() : null;
            if(targetName == null) continue;

            // Check for the ':'
            boolean embeddedColon = ( targetName.charAt( targetName.length() - 1 ) == ':' );
            if(embeddedColon) {
                targetName = targetName.substring( 0, targetName.length() - 1 );
            }
            else {
                if( !matcher.find ()             ) continue;
                if( !matcher.group().equals(":") ) continue;
            }

            // Resolve absolute path for the target
            targetName = SysUtil.resolvePath(targetName, depFileAbsDir);

            // Get the dependencies
            final String          srcDir  = SysUtil.getCWD();
            final TreeSet<String> depList = new TreeSet<>();

            while( matcher.find() ) {
                // Generate the dependency path
                final String depPath = SysUtil.resolvePath( matcher.group(), smCpp20Module ? depFileAbsDir : srcDir );
                // If not currently building a dependency list for a C++20 module, check if the path is already
                // in the list of C++20 modules
                if( !forTestOnly && !smCpp20Module && !_globalCpp20ModSet.isEmpty() ) {
                    // Get the file extension as needed
                    if(cppObjExt == null) cppObjExt = '.' + SysUtil.getFileExtension( _globalCpp20ModSet.first() );
                    // Generate the check path
                    final String depPathChk = SysUtil.resolvePath(matcher.group(), depFileAbsDir) + cppObjExt;
                    // If the path is already in the list of C++20 modules, skip it
                    if( _globalCpp20ModSet.contains(depPathChk) ) continue;
                }
                // Add the path to the dependency list
                depList.add(depPath);
            }

            if( depList.isEmpty() ) continue;

            // Add to the temporary map
            depMap.put(targetName, depList);

            // If currently building a dependency list for a C++20 module, add the target to the list of C++20 modules
            if(!forTestOnly && smCpp20Module) _globalCpp20ModSet.add(targetName);

            // Print debugging information as needed
            if(debug) {
                SysUtil.stdOut().println("[[[ " + targetName + " ]]]");
                for(final String item : depList) SysUtil.stdOut().println(item);
                SysUtil.stdOut().println();
            }

        } // while true

        // Check for dependency recursion
        for( final String target : depMap.keySet() ) {

            final String errPreq = _checkDependencyRecursion(0, depMap, target, null);
            if(errPreq != null) throw XCom.newIOException(
                Texts.EMsg_LoadDepDataLevelOrRec, SysUtil.resolveRelativePath(target), SysUtil.resolveRelativePath(errPreq)
            );
        }

        // Return the temporary map
        return depMap;
    }

    private static TreeMap< String, TreeSet<String> > _loadDepData(final String depFileAbsDir, final BufferedReader bfr, final boolean debug) throws IOException
    { return _loadDepData(depFileAbsDir, bfr, debug, false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static String testLoadDepDataStr(final String depDataStr, final String depOutFilePath, final boolean debug)
    {
        // Get the absolute paths
        final String depFileAbsPath = SysUtil.resolveAbsolutePath(depOutFilePath);
        final String depFileAbsDir  = SysUtil.getDirName(depFileAbsPath);

        // Test load the dependency list(s)
        try {
            _loadDepData( depFileAbsDir, new BufferedReader( new StringReader(depDataStr) ), debug, true );
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // On error, return null
            return null;
        }

        // On success, return the absolute path of the dependency list output file
        return depFileAbsPath;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static TreeMap< String, TreeSet<String> > loadDepFile(final String path, boolean skipIfNotExist) throws IOException
    {
        // Get the absolute paths
        final String depFileAbsPath = SysUtil.resolveAbsolutePath(path);
        final String depFileAbsDir  = SysUtil.getDirName(depFileAbsPath);

        // Check if the path actually exists
        if(skipIfNotExist) {
            if( !SysUtil.pathIsValidFile(depFileAbsPath) ) return null;
        }

        // Open the file
        final BufferedReader bfr = new BufferedReader( new InputStreamReader( new FileInputStream(depFileAbsPath), SysUtil._CharEncoding ) );

        // Load the dependency data
        final TreeMap< String, TreeSet<String> > dm = _loadDepData(depFileAbsDir, bfr, false);

        // Close the file
        bfr.close();

        // Return the dependency data
        return dm;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static void globalLoadDepFile(final String path, boolean skipIfNotExist) throws IOException
    {
        // Load the dependency list(s)
        final TreeMap< String, TreeSet<String> > depMap = loadDepFile(path, skipIfNotExist);

        // Exit here if no map was loaded
        if(depMap == null) return;

        // Add to the global map
        for( final Map.Entry< String, TreeSet<String> > item : depMap.entrySet() ) {

            final TreeSet<String> chkEntry = _globalDepMap.get( item.getKey() );

            if(chkEntry != null) {
                for( final String absPath : item.getValue() ) {
                    chkEntry.add(absPath);
                }
            }

            else {
                _globalDepMap.put( item.getKey(), item.getValue() );
            }

        } // for
    }

} // class GlobalDepLoad
