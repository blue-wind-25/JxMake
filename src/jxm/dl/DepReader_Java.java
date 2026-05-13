/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.dl;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

    // Pattern to detect fully qualified class names (e.g. org.my_package.MyClass)
    private static final Pattern _pmFQNClassName   = Pattern.compile("\\b([a-z][a-zA-Z0-9_]*(?:\\.[a-z][a-zA-Z0-9_]*)*\\.[A-Z][a-zA-Z0-9_]*)\\b");

    // Pattern to detect reflection-based constructs to exclude from FQN scanning
    private static final Pattern _pmReflection     = Pattern.compile("\\b(?:Class|Method|Field|Constructor)\\.(?:forName|invoke|newInstance|getMethod|getField|getConstructor|getDeclared[A-Za-z]*)\\s*\\(");

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

    private final HashSet  <String>  _procDirList = new HashSet   <>();
    private final Deque    <String>  _dirFileList = new ArrayDeque<>();

    private final SymbolMap          _symPathMap  = new SymbolMap   ();

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
        final StringBuilder   sb = new StringBuilder  ( str.length() );
        final StringTokenizer st = new StringTokenizer( str, "."     );

        while( st.hasMoreTokens() ) {
            if( sb.length() > 0 ) sb.append('/');
            sb.append( st.nextToken() );
        }

        return sb.toString();
    }

    @Override
    public String readOneDepPath() throws IOException
    {
        // Loop until a path is found or EOF
        while(true) {

            // Return from the existing directory file list if it is not empty
            if( !_dirFileList.isEmpty() ) return _dirFileList.removeFirst();

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
                // Warn only when the parent package directory is reachable (project package) but the specific class file
                // is absent - avoids noise for library imports
                if( _warnMissingDependency() ) {
                    final int lastSlash = filePath.lastIndexOf('/');
                    if(lastSlash > 0) {
                        final String pkgDir = filePath.substring(0, lastSlash);
                        if( _resolveRelDepDirPath(pkgDir) != null || _resolveAbsDepDirPath(pkgDir) != null ) {
                             SysUtil.printfSimpleWarning( Texts.WMsg_JavaDepNotFound, filePath, filePath() );
                        }
                    }
                }
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

            // ##### !!! TODO : Handle 'import static' !!! #####

             // Strip string/char literals and reflection-based constructs before scanning
             final String strippedLine = _pmReflection.matcher( _pmJavaSQDQ.matcher(line).replaceAll("") ).replaceAll("");

             // Scan for symbol names from the same-directory and wildcard-imported packages
             final Matcher jsnMatcher = _pmJavaSymbolName.matcher(strippedLine);
             while( jsnMatcher.find() ) {
                // Get the paths
                final HashSet<String> paths = _symPathMap.get( jsnMatcher.group(1) );
                if(paths == null) continue;
                // Add the paths
                for(final String path : paths) {
                    if( _dirFileList.contains(path) ) continue;
                    _dirFileList.add(path);
                }
             } // while

             // Scan for fully qualified class names (e.g. org.my_package.MyClass)
             final Matcher fqnMatcher = _pmFQNClassName.matcher(strippedLine);
             while( fqnMatcher.find() ) {
                // Get the file path for the FQN
                final String fqnFilePath = _convertDotToPath( fqnMatcher.group(1) ) + ".java";
                // Try to resolve using relative location, then absolute
                String fqnDepPath = _resolveRelDepFilePath(fqnFilePath);
                if(fqnDepPath == null) fqnDepPath = _resolveAbsDepFilePath(fqnFilePath);
                // Add to the pending list if found
                if(fqnDepPath != null) {
                    if( !_dirFileList.contains(fqnDepPath) ) _dirFileList.add(fqnDepPath);
                }
            } // while

            /*
            // Terminate the file read if the keyword 'class', 'interface', or 'module' is found
            if( _pmTerminate.matcher(line).find() ) return null;
            //*/

        } // while
    }

} // class DepReader_Java
