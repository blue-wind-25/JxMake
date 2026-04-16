/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.dl;


import java.util.ArrayList;
import java.util.List;

import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import jxm.*;
import jxm.xb.*;


public abstract class DepReader {

    private final String            _filePath;
    private final String            _dirPath;
    private final BufferedReader    _bfr;

    private final ArrayList<String> _searchPaths = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * Useful tools:
     *     https://regex101.com/r/V5kllq/1
     *
     *     https://www.debuggex.com
     *
     *     https://bisqwit.iki.fi/source/regexopt.html
     *     git clone git://bisqwit.iki.fi/regex-opt.git
     */
                                                                   // "^(?:ccm?|cpm?|cppm?|cxxm?|c\\+\\+m?|CM?|CCM?|CPM?|CPPM?|CXXM?|C\\+\\+M?)$"
    protected static final Pattern _pmCppSrcExt     = Pattern.compile("^(?:ccm?|c(?:p{1,2}|xx|\\+\\+)m?|C(?:C|P{1,2}|XX|\\+\\+)?M?)$"                          );
    protected static final Pattern _pmJavaSrcExt    = Pattern.compile("^java$"                                                       , Pattern.CASE_INSENSITIVE);

                                                                   // ".+\\.(?:h|hh|hp|hpp|hxx|h\\+\\+tpp|tcc|c|cc|cp|cpp|cxx|c\\+\\+)$"
    protected static final Pattern _pmMatchCFile    = Pattern.compile(".+\\.(?:[htc]pp|[hc](?:xx|p)?|[hc]\\+\\+|hh|t?cc)$"           , Pattern.CASE_INSENSITIVE);
    protected static final Pattern _pmMatchJavaFile = Pattern.compile(".+\\.java$"                                                   , Pattern.CASE_INSENSITIVE);

    protected static final String  _smCppInclude    = "#!/type/C++ Include";
    protected static final String  _smCpp20Module   = "#!/type/C++20 Module";
    protected static final String  _smJavaImport    = "#!/type/Java Module";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static StringBuilder _transformPathCommon(final String name)
    {
        final StringBuilder sb = new StringBuilder();

        for( int i = 0; i < name.length(); ++i ) {
            final char ch = name.charAt(i);
            if( Character.isWhitespace(ch) ) sb.append('\\');
            sb.append(ch);
        }

        return sb;
    }

    protected static String _transformCppIncludePath(final String name, final boolean forTarget, final String objFileExt)
    {
        final StringBuilder sb = _transformPathCommon(name);

        if(forTarget) {
            sb.append('.'       );
            sb.append(objFileExt);
        }

        return sb.toString();
    }

    protected static String _transformJavaImportPath(final String name, final boolean forTarget)
    {
        final StringBuilder sb = _transformPathCommon(name);

        if(forTarget) {
            final int extIdx = sb.lastIndexOf(".java");
            if(extIdx != -1) sb.replace(extIdx + 1, extIdx + 5, "class");
        }

        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static String _transformCppModulePath(final String name, boolean isHeaderUnit, boolean isLocal, final String objFileExt)
    {
        final StringBuilder sb = _transformPathCommon(name);

        sb.append('.');
        if(isHeaderUnit) {
            sb.append(isLocal ? "usr" : "sys");
            sb.append(".unit");
        }
        else {
            sb.append(objFileExt);
        }

        return sb.toString();
    }

    protected static String _transformCppModulePath(final String name, boolean isHeaderUnit, boolean isLocal)
    { return _transformCppModulePath(name, isHeaderUnit, isLocal, null); }

    protected static String _transformCppModulePath(final String name, final String objFileExt)
    { return _transformCppModulePath(name, false, false, objFileExt); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static interface GDLTransformPath {
        public String transformPath(final String pathStr, final boolean forTarget);
    }

    protected static void _gdlSimple_impl(final String depFileType, final String depOutFilePath, final String sourceDirPath, final ArrayList<String> searchPaths, final Pattern pmMatchExt, final GDLTransformPath gdltp, final String buildDir) throws IOException
    {
        // List the source directory path
        final List<String> files = SysUtil.cu_lsfile_rec( SysUtil.resolveAbsolutePath(sourceDirPath), pmMatchExt );

        // Prepare the string builder that will hold the dependency data that can be loaded using 'depload' and 'sdepload'
        final StringBuilder sb = new StringBuilder();

        // Put the dependency list type
        sb.append(depFileType);
        sb.append('\n');
        sb.append('\n');

        // Check if it is for C++20 module
        final boolean smCpp20Module = depFileType.equals(DepReader._smCpp20Module);

        // Process the files
        for(final String file : files) {

            // Store the target
            final String targetRelPath = smCpp20Module
                                       ? gdltp.transformPath( SysUtil.resolvePath( SysUtil.resolveRelativePath(file), buildDir ), true )
                                       : gdltp.transformPath(                      SysUtil.resolveRelativePath(file)            , true );

            sb.append(targetRelPath);
            sb.append(':'          );

            // Process the dependencies
            for( final String path : DepBuilder.buildDepList(file, searchPaths, searchPaths).getDepFiles() ) {
                final String preqRelPath = gdltp.transformPath( SysUtil.resolveRelativePath(path), false );
                sb.append(' '        );
                sb.append(preqRelPath);
            }

            // Add newlines
            sb.append('\n');
            sb.append('\n');

        } // for

        // Test and save the result
        _testAndSaveDepDataStr( sb.toString(), depOutFilePath );
    }

    protected static void _testAndSaveDepDataStr(final String depDataStr, final String depOutFilePath) throws IOException
    {
        // Test the data
        final String depFileAbsPath = GlobalDepLoad.testLoadDepDataStr(depDataStr, depOutFilePath, false);

        if(depFileAbsPath == null) throw XCom.newIOException(Texts.EMsg_GenDepDataFailed, depOutFilePath);

        // Save the data
        final BufferedWriter bfw = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(depFileAbsPath), SysUtil._CharEncoding ) );

        bfw.write(depDataStr);
        bfw.flush(          );
        bfw.close(          );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected DepReader(final String sourceFilePath, final ArrayList<String> searchPaths) throws IOException
    {
        _filePath = SysUtil.resolveAbsolutePath(sourceFilePath);
        _dirPath  = SysUtil.getDirName(_filePath);
        _bfr      = new BufferedReader( new InputStreamReader( new FileInputStream(_filePath), SysUtil._CharEncoding ) );

        if(searchPaths != null) {
            for(final String path : searchPaths) _searchPaths.add( SysUtil.resolveAbsolutePath(path) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public String filePath()
    { return _filePath; }

    public String dirPath()
    { return _dirPath; }

    public abstract String readOneDepPath() throws IOException;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static DepReader newDepReader(final String sourceFilePath, final ArrayList<String> absCIncludePaths, final ArrayList<String> absJavaClassPaths) throws IOException
    {
        // Check for C/C++ source code file
        if( _pmMatchCFile.matcher(sourceFilePath).matches() ) {
            // Create and return the instance
            return new DepReader_C(sourceFilePath, absCIncludePaths);
        }

        // Check for Java source code file
        if( _pmMatchJavaFile.matcher(sourceFilePath).matches() ) {
            // Create and return the instance
            return new DepReader_Java(sourceFilePath, absJavaClassPaths);
            /*
            // Create the instance
            final DepReader depReader = new DepReader_Java(sourceFilePath, searchPaths);
            // Add the directory of the source file to the search path as needed
            if( !searchPaths.contains( depReader.dirPath() ) ) searchPaths.add( 0, depReader.dirPath() );
            // Return the instance
            return depReader;
            //*/
        }

        // Unsupported source code file
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _readLine_impl() throws IOException
    {
        // Prepare the line buffer
        String lineStr = ""; // NOTE : Do not use 'StringBuilder' here because the chance of '\\' appearing would be quite rare

        // Read and combine line(s)
        while(true) {

            // Read one line
            final String line = _bfr.readLine();

            if(line == null) {
                // Close the file
                _bfr.close();
                break;
            }

            // Check if the line is empty
            if( line.length() == 0 ) continue;

            // Check if the last character is the line continuation character
            if( line.charAt( line.length() - 1 ) == '\\' ) {
                // Concatenate the line (minus the last character)
                lineStr += line.substring( 0, line.length() - 1 );
            }
            // The last character is not the line continuation character
            else {
                lineStr += line;
                break;
            }

        } // while true

        // If the line string length is zero, it means the file has reached EOF
        return ( lineStr.length() == 0 ) ? null : lineStr;
    }

    protected String _readLine_CppJava() throws IOException
    {
        // A flag to indicate whether it is currently inside a multiline comment block
        boolean inMLC = false;

        // Loop until a non-comment line is acquired
        while(true) {

            // Read one line
            String line = _readLine_impl();
            if(line == null) return null;

            // Trim the line
            line = line.trim();

            // Check for '//' as needed
            if(!inMLC) {
                final int slIdx = line.indexOf("//");
                if(slIdx >= 0) {
                    line = line.substring(0, slIdx).trim();
                    if( line.isEmpty() ) continue;
                }
            }

            // Check for '/*' as needed
            if(!inMLC) {
                final int mlBIdx = line.indexOf("/*");
                if(mlBIdx >= 0) {
                    final int mlEIdx = line.indexOf("*/");
                    if(mlEIdx > mlBIdx) {
                        line = ( line.substring(0, mlBIdx) + line.substring(mlEIdx + 2) ).trim();
                    }
                    else {
                        inMLC = true;
                        line = line.substring(0, mlBIdx).trim();
                    }
                    if( line.isEmpty() ) continue;
                }
            }

            // Check for '*/' as needed
            if(inMLC) {
                final int mlEIdx = line.indexOf("*/");
                if(mlEIdx >= 0) {
                    inMLC = false;
                    line = line.substring(mlEIdx + 2).trim();
                    if( line.isEmpty() ) continue;
                }
            }

            // Only return the line only if it is currently outside a multiline comment block
            if(!inMLC) return line;

        } // while
    }

    private String _resolveAbsDepPath_impl(final String depName, final boolean isFile)
    {
        // Walk through the search paths
        for(final String searchPath : _searchPaths) {

            // Resolve using the search path
            final String absDepPath = SysUtil.resolvePath(depName, searchPath);

            // Check if the dependency path do exist
            if( SysUtil.pathIsValid(absDepPath) ) {
                if(  isFile && SysUtil.pathIsFile     (absDepPath) ) return absDepPath;
                if( !isFile && SysUtil.pathIsDirectory(absDepPath) ) return absDepPath;
            }

        } // for

        // The dependency path does not exist
        return null;
    }

    private String _resolveRelDepPath_impl(final String depName, final boolean isFile)
    {
        // Resolve using the current directory
        final String absDepPath = SysUtil.resolvePath(depName, _dirPath);

        // Check if the dependency path do exist
        if( SysUtil.pathIsValid(absDepPath) ) {
            if(  isFile && SysUtil.pathIsFile     (absDepPath) ) return absDepPath;
            if( !isFile && SysUtil.pathIsDirectory(absDepPath) ) return absDepPath;
        }

        // The dependency path does not exist
        return null;
    }

    protected String _resolveAbsDepFilePath(final String depName)
    { return _resolveAbsDepPath_impl(depName, true); }

    protected String _resolveRelDepFilePath(final String depName)
    { return _resolveRelDepPath_impl(depName, true); }

    protected String _resolveAbsDepDirPath(final String depName)
    { return _resolveAbsDepPath_impl(depName, false); }

    protected String _resolveRelDepDirPath(final String depName)
    { return _resolveRelDepPath_impl(depName, false); }

} // class DepReader
