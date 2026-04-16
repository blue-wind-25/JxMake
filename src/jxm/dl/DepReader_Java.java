/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.dl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;

import jxm.*;
import jxm.xb.*;


public class DepReader_Java extends DepReader {

    private static final Pattern _pmImportOne      = Pattern.compile("import\\s*([^*\\r\\n]*)\\s*;"                  ); // import package.class
    private static final Pattern _pmImportAll      = Pattern.compile("import\\s*([^*\\r\\n]*)\\s*\\.\\s*\\*\\s*\\s*;"); // import package.*

    private static final Pattern _pmJavaSQDQ       = Pattern.compile("([\"'])(?:[^\\\"]|\\.)*?\\1");
    private static final Pattern _pmJavaSymbolName = Pattern.compile('(' + XCom._reStrSymbolNameUnicode + ')', Pattern.UNICODE_CHARACTER_CLASS);

  //private static final Pattern _pmTerminate      = Pattern.compile("\\b(?:class|interface|module)\\b");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final XCom.Mutex                      _lsfMutex = new XCom.Mutex();
    private static final HashMap< String, List<String> > _lsfCache = new HashMap<>();

    private static List<String> _lsJavaFileInDir(final String dirPath) throws IOException
    {
        // Lock mutex
        _lsfMutex.lock();

        // Try to get the file list from the cache first
        List<String> files = _lsfCache.get(dirPath);

        // List the directory if the file list does not exist in the cache
        if(files == null) {
            try {
                files = SysUtil.cu_lsfile(dirPath, _pmJavaSrcExt);
            }
            catch(final IOException e) {
                // Unlock mutex
                _lsfMutex.unlock();
                // Re-throw the exception
                throw e;
            }
            // Store the file list to the cache
            _lsfCache.put(dirPath, files);
        }

        // Unlock mutex
        _lsfMutex.unlock();

        // Return the file list
        return files;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    private class SymbolMap extends HashMap< String, HashSet<String> > {
        public void putItem(final String symbolName, final String sourcePath)
        {
            //SysUtil.stdDbg().printf("### %s : %s\n", symbolName, sourcePath);

            HashSet<String> paths = get(symbolName);

            if(paths == null) {
                paths = new HashSet<>();
                put(symbolName, paths);
            }

            paths.add(sourcePath);
        }
    }

    private final HashSet  <String> _procDirList = new HashSet  <>();
    private final ArrayList<String> _dirFileList = new ArrayList<>();

    private final SymbolMap         _symPathMap  = new SymbolMap  ();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static ArrayList<String> _addCWDToSearchPath(final ArrayList<String> searchPaths)
    {
        if( searchPaths != null && !searchPaths.contains(".") && !searchPaths.contains( SysUtil.getCWD() ) ) {
            // Ensure the project root directory is included in the search path
            searchPaths.add(0, ".");
        }

        return searchPaths;
    }

    protected DepReader_Java(final String sourceFilePath, final ArrayList<String> searchPaths) throws IOException
    { super( sourceFilePath, _addCWDToSearchPath(searchPaths) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void gdlJavaImport(final String depOutFilePath, final String sourceDirPath, final ArrayList<String> searchPaths, final String buildDir) throws IOException
    { _gdlSimple_impl( DepReader._smJavaImport, depOutFilePath, sourceDirPath, searchPaths, _pmJavaSrcExt, (ps, ft) -> { return _transformJavaImportPath(ps, ft); }, buildDir ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _convertDotToPath(final String str)
    {
        String cnv = "";

        final StringTokenizer st = new StringTokenizer(str, ".");

        while( st.hasMoreTokens() ) {
            if( !cnv.isEmpty() ) cnv += '/';
            cnv += st.nextToken();
        }

        return cnv;
    }

    @Override
    public String readOneDepPath() throws IOException
    {
        // Loop until a path is found or EOF
        while(true) {

            // Return from the existing directory file list if it is not empty
            if( !_dirFileList.isEmpty() ) return _dirFileList.remove(0);

            // Read one line
            final String line = _readLine_CppJava();
            if(line == null) return null;

            // Check for 'import a.b.c.d.e.ClassName;'
            final Matcher oneMatcher = _pmImportOne.matcher(line);
            if( oneMatcher.matches() ) {
                // Get the file path
                final String filePath = _convertDotToPath( oneMatcher.group(1) ) + ".java";
                // Try to resolve using relative location
                final String relDepPath = _resolveRelDepFilePath(filePath);
                if(relDepPath != null) return relDepPath;
                // Try to resolve using absolute location
                final String absDepPath = _resolveAbsDepFilePath(filePath);
                if(absDepPath != null) return absDepPath;
            }

            // Check for 'import a.b.c.d.e.*;'
            final Matcher allMatcher = _pmImportAll.matcher(line);
            if( allMatcher.matches() ) {
                // Get the file path
                final String dirPathPart = _convertDotToPath( allMatcher.group(1) );
                      String dirPath     = null;
                // Try to resolve using relative and absolute location
                                    dirPath = _resolveRelDepDirPath(dirPathPart);
                if(dirPath == null) dirPath = _resolveAbsDepDirPath(dirPathPart);
                // Only process if the path has not been processed before for this current file
                if( dirPath != null && !_procDirList.contains(dirPath) ) {
                    // Add the path to the already processed list
                    _procDirList.add(dirPath);
                    // List directory
                    final List<String> files = _lsJavaFileInDir(dirPath);
                    if(files == null) continue;
                    // Add the files to the map
                    for(final String fileName : files) {
                        final String filePath = SysUtil.resolvePath(fileName, dirPath);
                        _symPathMap.putItem( SysUtil.getFileNameWithoutExtension(filePath), filePath );
                    }
                }
            }

            // Check all '*.java' files in the same directory as the source file
            if(true) {
                // Get the path
                final String dirPath = dirPath();
                // Only process if the path has not been processed before for this current file
                if( !_procDirList.contains(dirPath) ) {
                    // Add the path to the already processed list
                    _procDirList.add(dirPath);
                    // List the directory
                    final List<String> files = _lsJavaFileInDir(dirPath);
                    if(files == null) continue;
                    // Add the files to the list
                    for(final String fileName : files) {
                        final String filePath = SysUtil.resolvePath(fileName, dirPath);
                        _symPathMap.putItem( SysUtil.getFileNameWithoutExtension(filePath), filePath );
                    }
                }
            }

            /*
             * ### !!! TODO : !!! ###
             *     + Handle 'import static'!!!
             *     + It can still cause circular dependencies! How to fix?
             *     + Not all possible conditions may have been considered (directly using the fully qualified name, reflection, etc.)
             */
             final Matcher m = _pmJavaSymbolName.matcher( _pmJavaSQDQ.matcher(line).replaceAll("") );
             while( m.find() ) {
                // Get the paths
                final HashSet<String> paths = _symPathMap.get( m.group(1) );
                if(paths == null) continue;
                // Add the paths
                for(final String path : paths) {
                    if( _dirFileList.contains(path) ) continue;
                    _dirFileList.add(path);
                }
             } // while

            /*
            // Terminate the file read if the keyword 'class', 'interface', or 'module' is found
            if( _pmTerminate.matcher(line).find() ) return null;
            //*/

        } // while
    }

} // class DepReader_Java
