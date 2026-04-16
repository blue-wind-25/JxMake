/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


/*
import java.awt.AWTError;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
//*/

import java.io.DataInputStream;
import java.io.FileInputStream;

import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;


public class ArgParser {

    public static final String JMX_DefaultStartPath = ".";
    public static final String JMX_DefaultSpecFile  = "JxMakeFile";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class VarArg {
        String name;
        String value;

        public VarArg(final String name_, final String value_)
        {
            name  = name_;
            value = value_;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ArrayList<String> _savedArgs                     = new ArrayList<>();

    private       String            _jmxStartPath                  = JMX_DefaultStartPath;
    private       String            _jmxSpecFile                   = JMX_DefaultSpecFile;

    private final ArrayList<String> _jmxTargets                    = new ArrayList<>();
    private final ArrayList<VarArg> _jmxVarArgs                    = new ArrayList<>();

    private       boolean           _silentSuppressedError         = false;

    private       boolean           _enableJXMLibLoadVerbose       = false;

    private       boolean           _enableJXMSpecFileCache        = false;
    private       boolean           _enableJXMSpecFileCacheVerbose = false;

    private       boolean           _deleteJXMSpecFileCacheAndExit = false;
    private       boolean           _deleteProjectFileCacheAndExit = false;

    private       boolean           _useLightColorTheme            = false;
    private       boolean           _useLightColorThemeGUI         = true;
    private       boolean           _disableANSIEscapeCode         = false;

    private       boolean           _enableAllExceptionStackTrace  = false;

    private       boolean           _enableWarnEvalInvRefVar       = false;
    private       boolean           _enableWarnEvalVarNotExist     = false;

    private       boolean           _enableWarnCnvStringInteger    = false;
    private       boolean           _enableWarnCnvStringBoolean    = false;

    private       boolean           _enableHeadless                = false;
    private       boolean           _enableOpenGL                  = false;
    private       boolean           _enableDirect3D                = false;

    private       boolean           _extractEJARLibAndExit         = false;
    private       boolean           _extractEJARDocsAndExit        = false;
    private       boolean           _extractEJARABrdDecAndExit     = false;

    private       boolean           _compileJMXSpecBinFileAndExit  = false;

    private       boolean           _enableDummyTargetPreq         = false;

    private       boolean           _runDocumentBrowser            = false;
    private       boolean           _runScriptEditor               = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ArrayList<String> savedArgs()
    { return _savedArgs; }

    public String jxmStartPath()
    { return _jmxStartPath; }

    public String jxmSpecFile()
    { return _jmxSpecFile; }

    public ArrayList<String> jmxTargets()
    { return _jmxTargets; }

    public ArrayList<VarArg> jmxVarArgs()
    { return _jmxVarArgs; }

    public boolean silentSuppressedError()
    { return _silentSuppressedError; }

    public boolean enableJXMLibLoadVerbose()
    { return _enableJXMLibLoadVerbose; }

    public boolean enableJXMSpecFileCache()
    { return _enableJXMSpecFileCache; }

    public boolean enableJXMSpecFileCacheVerbose()
    { return _enableJXMSpecFileCacheVerbose; }

    public boolean deleteJXMSpecFileCacheAndExit()
    { return _deleteJXMSpecFileCacheAndExit; }

    public boolean deleteProjectFileCacheAndExit()
    { return _deleteProjectFileCacheAndExit; }

    public boolean useLightColorTheme()
    { return _useLightColorTheme; }

    public boolean useLightColorThemeGUI()
    { return _useLightColorThemeGUI; }

    public boolean disableANSIEscapeCode()
    { return _disableANSIEscapeCode; }

    public boolean enableAllExceptionStackTrace()
    { return _enableAllExceptionStackTrace; }

    public boolean enableWarnEvalInvRefVar()
    { return _enableWarnEvalInvRefVar; }

    public boolean enableWarnEvalVarNotExist()
    { return _enableWarnEvalVarNotExist; }

    public boolean enableWarnCnvStringInteger()
    { return _enableWarnCnvStringInteger; }

    public boolean enableWarnCnvStringBoolean()
    { return _enableWarnCnvStringBoolean; }

    public boolean enableHeadless()
    { return _enableHeadless; }

    public boolean enableOpenGL()
    { return _enableOpenGL; }

    public boolean enableDirect3D()
    { return _enableDirect3D; }

    public boolean extractEJARLibAndExit()
    { return _extractEJARLibAndExit; }

    public boolean extractEJARDocsAndExit()
    { return _extractEJARDocsAndExit; }

    public boolean extractEJARABrdDecAndExit()
    { return _extractEJARABrdDecAndExit; }

    public boolean compileJMXSpecBinFileAndExit()
    { return _compileJMXSpecBinFileAndExit; }

    public boolean enableDummyTargetPreq()
    { return _enableDummyTargetPreq; }

    public boolean runDocumentBrowser()
    { return _runDocumentBrowser; }

    public boolean runScriptEditor()
    { return _runScriptEditor; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _getCompilerJDKVersion()
    {
        try {

            DataInputStream dis = null;

            if( SysUtil.isRunFromClassFile() ) {
                dis = new DataInputStream ( new FileInputStream("jxm/JxMake.class") );
            }
            else {
                dis = new DataInputStream ( JxMake.class.getClassLoader().getResourceAsStream("jxm/JxMake.class") );
            }

            final int magic = dis.readInt          (); // 0xCAFEBABE
            final int minor = dis.readUnsignedShort();
            final int major = dis.readUnsignedShort();

            dis.close();

            // Please refer to "https://docs.oracle.com/javase/specs/jvms/se19/html/jvms-4.html#jvms-4.1-200-B.2" for more details
            if(major >= 49) return        String.valueOf(major - 44);
            else            return "1." + String.valueOf(major - 44);

        } // try
        catch(final Exception e) {}

        return Texts.IMsg_ArgParserUnknownJDKVersion;
    }

    private static String[] _getProgCmd()
    {
        /*
        String cmd = "java -jar jxmake.jar";

        try {
            if( SysUtil.isRunFromJARClass() || SysUtil.isRunFromClassFile() ) {
                final String main = System.getProperty("sun.java.command");
                if( main != null && !main.endsWith(".jar") ) cmd = "java " + main.split(" ")[0];
                /*
                final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
                if(trace.length > 0) cmd = "java " + trace[trace.length - 1].getClassName();
                * /
            }
            // Assume it was run from a jar file
            else {
                cmd = "java -jar " + SysUtil.getFileName( SysUtil.getJxMakeJarURI().getPath() );
            }
        }
        catch(final Exception e) {}
        */

        return new String[] {
            "jxmake"                        ,
            "jxmake.sh"                     ,
            "jxmake.bat"                    ,
            "java -cp <CLASSPATHS> JXM_Main",
            "java -jar jxmake.jar"
        };
    }

    public void showUsageAndExit(final String errMsg)
    {
        // Enable hardware headless mode if asked
        if( this.enableHeadless() ) System.setProperty("java.awt.headless", "true");

        // NOTE : Synchronize this with the default destination directory in 'SysUtil.copyJARRes_embeddedDoc()'
        final String extractDocDir = SysUtil.osIsWindows() ? "%USERPROFILE%\\" + SysUtil._JxMakeDataRoot + "\\docs"
                                                           : "$HOME/"          + SysUtil._JxMakeDataRoot + "/docs";

        // NOTE : Synchronize this with the default destination directory in 'SysUtil.copyJARRes_embeddedLib()'
        final String extractLibDir = SysUtil.osIsWindows() ? "%USERPROFILE%\\" + SysUtil._JxMakeDataRoot + "\\lib"
                                                           : "$HOME/"          + SysUtil._JxMakeDataRoot + "/lib";

        // NOTE : Synchronize this with the default destination directory in 'SysUtil.copyJARRes_embeddedABrdDec()'
        final String extractArdDec = SysUtil.osIsWindows() ? "%USERPROFILE%\\" + SysUtil._JxMakeDataRoot

                                                           : "$HOME/"          + SysUtil._JxMakeDataRoot;

        // Print the usage help text
        Texts.printUsageHelp(
            errMsg, _getProgCmd(), JMX_DefaultSpecFile, extractDocDir, extractLibDir, extractArdDec, SysUtil._JxMakeTmpDirRoot
        );

        /*
        for( final String s : SysUtil.getJavaCmd() ) SysUtil.stdDbg().println(s);
        //*/

        if(errMsg != null) SysUtil.systemExitError();
        else               SysUtil.systemExit();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("this-escape")
    public ArgParser(final String[] args) throws Exception
    {

        /*
        // NOTE : This code will actually postpone the exception and trigger it later down the line!
        // Check if the JVM is actually running in headless mode
        boolean checkHeadless = GraphicsEnvironment.isHeadless();

        if(!checkHeadless) {
            try { Toolkit.getDefaultToolkit(); }
            catch(final AWTError e) { checkHeadless = true; }
        }

        if(checkHeadless) {
            _savedArgs.add("--en-headless");
            _enableHeadless = true;
        }
        //*/

        // Check if no arguments are specified
        if(args.length == 0) {
            if( !SysUtil.pathIsValidFile(JMX_DefaultSpecFile) ) {
                showUsageAndExit( String.format(Texts.IMsg_ArgParserNoArgNoDefSpecFile, JMX_DefaultSpecFile) );
            }
            return;
        }

        // Process the argument(s)
        final StringBuilder sbPrg = new StringBuilder();

        for(int i = 0; i < args.length; ++i) {

            final String arg0   =                         args[i    ].trim()     ;
            final String arg1NT = (i + 1 < args.length) ? args[i + 1]        : "";
            final String arg1   = arg1NT.trim();

            switch(arg0) {

                // Show the usage message and exit
                case "-h":
                    // Show the usage message and exit
                    showUsageAndExit(null);
                    break;

                // Silence any suppressed error messages
                case "-s":
                    // Save the argument as needed
                    if(!_silentSuppressedError) _savedArgs.add(arg0);
                    // Set flag
                    _silentSuppressedError = true;
                    break;

                // Specify the JxMake start path
                case "-C":
                    // Check if there is no more argument
                    if( arg1.isEmpty() ) showUsageAndExit(Texts.IMsg_ArgParserOptionCReqArg);
                    // Save the start path and increment the counter
                    _jmxStartPath = arg1;
                    ++i;
                    break;

                // Specify the JxMake specification file
                case "-f":
                    // Check if there is no more argument
                    if( arg1.isEmpty() ) showUsageAndExit(Texts.IMsg_ArgParserOptionFReqArg);
                    // Save the file name and increment the counter
                    _jmxSpecFile = arg1;
                    ++i;
                    // Save the arguments
                    _savedArgs.add(arg0);
                    _savedArgs.add( SysUtil.getFileName(arg1) );
                    break;

                // Execute the given program text
                case "-e":
                    // Check if there is no more argument
                    if( arg1NT.isEmpty() ) showUsageAndExit(Texts.IMsg_ArgParserOptionEReqArg);
                    // Append the program text
                    sbPrg.append(arg1NT);
                    sbPrg.append("\n");
                    ++i;
                    break;

                case "--en-jxm-lload-verbose":
                    // Save the argument as needed
                    if(!_enableJXMLibLoadVerbose) _savedArgs.add(arg0);
                    // Set flag
                    _enableJXMLibLoadVerbose = true;
                    break;

                // Enable compiled JxMake specification file cache
                case "--en-jxm-cache":
                    // Save the argument as needed
                    if(!_enableJXMSpecFileCache) _savedArgs.add(arg0);
                    // Set flag
                    _enableJXMSpecFileCache = true;
                    break;

                case "--en-jxm-cache-verbose":
                    // Save the argument as needed
                    if(!_enableJXMSpecFileCacheVerbose) {
                        _savedArgs.add(arg0);
                        _savedArgs.remove("--en-jxm-cache");
                    }
                    // Set flags
                    _enableJXMSpecFileCache        = true;
                    _enableJXMSpecFileCacheVerbose = true;
                    break;

                // Delete the compiled JxMake specification file cache and exit
                case "--rm-jxm-cache":
                    // Set flag
                    _deleteJXMSpecFileCacheAndExit = true;
                    break;

                // Delete all the project's cache files and exit
                case "--rm-prj-cache":
                    // Set flag
                    _deleteProjectFileCacheAndExit = true;
                    break;

                // Print the program name and version and exit
                case "--version":
                    SysUtil.stdOut().printf( "JxMake %d.%d.%d", SysUtil.jxmVerMajor(), SysUtil.jxmVerMinor(), SysUtil.jxmVerPatch() );
                    if( SysUtil.jxmVerDevel().isEmpty() ) SysUtil.stdOut().printf( "\n"                           );
                    else                                  SysUtil.stdOut().printf( "-%s\n", SysUtil.jxmVerDevel() );
                    SysUtil.stdOut().printf( Texts.IMsg_JxMakeBuiltInfo, _getCompilerJDKVersion(), SysUtil.jxmCopyright() );
                    SysUtil.systemExit();
                    break;

                // Use dark color theme
                case "--dct":
                    // Save the argument as needed
                    if(!_useLightColorTheme) {
                        _savedArgs.add(arg0);
                        _savedArgs.remove("--lct");
                    }
                    // Set flag
                    _useLightColorTheme    = false;
                    _useLightColorThemeGUI = false;
                    break;

                // Use light color theme
                case "--lct":
                    // Save the argument as needed
                    if(!_useLightColorTheme) {
                        _savedArgs.add(arg0);
                        _savedArgs.remove("--dct");
                    }
                    // Set flag
                    _useLightColorTheme    = true;
                    _useLightColorThemeGUI = true;
                    break;

                // Disable the actual output of ANSI escape codes
                case "--ds-ansi-ec":
                    // Save the argument as needed
                    if(!_disableANSIEscapeCode) _savedArgs.add(arg0);
                    // Set flag
                    _disableANSIEscapeCode = true;
                    break;

                // Enable printing all exception stack trace on error
                case "--en-all-exception-st":
                    // Save the argument as needed
                    if(!_enableAllExceptionStackTrace) _savedArgs.add(arg0);
                    // Set flag
                    _enableAllExceptionStackTrace = true;
                    break;

                // Enable warning when evaluating eference variables which contain invalid references
                case "--en-warn-inv-ref-var":
                    // Save the argument as needed
                    if(!_enableWarnEvalInvRefVar) _savedArgs.add(arg0);
                    // Set flag
                    _enableWarnEvalInvRefVar = true;
                    break;

                // Enable warning when evaluating non-existent variables
                case "--en-warn-var-not-exist":
                    // Save the argument as needed
                    if(!_enableWarnEvalVarNotExist) _savedArgs.add(arg0);
                    // Set flag
                    _enableWarnEvalVarNotExist = true;
                    break;

                // Enable warning when converting strings to integers
                case "--en-warn-cnv-integer":
                    // Save the argument as needed
                    if(!_enableWarnCnvStringInteger) _savedArgs.add(arg0);
                    // Set flag
                    _enableWarnCnvStringInteger = true;
                    break;

                // Enable warning when converting strings to booleans
                case "--en-warn-cnv-boolean":
                    // Save the argument as needed
                    if(!_enableWarnCnvStringBoolean) _savedArgs.add(arg0);
                    // Set flag
                    _enableWarnCnvStringBoolean = true;
                    break;

                //  Enable all warnings
                case "--en-warn-all":
                    // Save the argument as needed
                    if( !_enableWarnEvalInvRefVar    ||
                        !_enableWarnEvalVarNotExist  ||
                        !_enableWarnCnvStringInteger ||
                        !_enableWarnCnvStringBoolean    ) _savedArgs.add(arg0);
                    // Set flags
                    _enableWarnEvalInvRefVar    = true;
                    _enableWarnEvalVarNotExist  = true;
                    _enableWarnCnvStringInteger = true;
                    _enableWarnCnvStringBoolean = true;
                    break;

                // Enable headless mode
                case "--en-headless":
                    // Save the argument as needed
                    if(!_enableHeadless) _savedArgs.add(arg0);
                    // Set flags
                    _enableHeadless = true;
                    break;

                // Enable OpenGL
                case "--en-opengl":
                    // Save the argument as needed
                    if(!_enableOpenGL) {
                        _savedArgs.add(arg0);
                        _savedArgs.remove("--en-d3d");
                    }
                    // Set flags
                    _enableOpenGL   = true;
                    _enableDirect3D = false;
                    break;

                // Enable Direct3D
                case "--en-d3d":
                    // Save the argument as needed
                    if(!_enableDirect3D) {
                        _savedArgs.add(arg0);
                        _savedArgs.remove("--en-opengl");
                    }
                    // Set flags
                    _enableOpenGL   = false;
                    _enableDirect3D = true;
                    break;

                // Extract the embedded JxMake loadable library files and exit
                case "--extract-ejar-lib":
                    _extractEJARLibAndExit = true;
                    break;

                // Extract the embedded JxMake documentation files and exit
                case "--extract-ejar-docs":
                    _extractEJARDocsAndExit = true;
                    break;

                // Extract the embedded 'SysUtil.ArduinoBoardsTxtDecRuleFile' file and exit
                case "--extract-ejar-abrd-dec":
                    _extractEJARABrdDecAndExit = true;
                    break;

                // Run the documentation browser and exit
                case "--browse-docs":
                    _runDocumentBrowser = true;
                    break;

                // Run the script editor and exit
                case "--script-editor":
                    _runScriptEditor = true;
                    break;

                // Compile the specified JxMake specification file to make a '*.bin' file and exit
                case "--__compile__":
                    _compileJMXSpecBinFileAndExit = true;
                    break;

                // Enable dummy target and prerequisites
                case "--__en_dtp__":
                    // Save the argument as needed
                    if(!_enableDummyTargetPreq) _savedArgs.add(arg0);
                    // Set flag
                    _enableDummyTargetPreq = true;
                    break;

                // Other arguments
                default:
                    // Check for invalid option
                    if( !arg0.isEmpty() && arg0.charAt(0) == '-' ) showUsageAndExit( String.format(Texts.IMsg_ArgParserInvalidOption, arg0) );
                    // Check for variable assigment
                    final String[] parts = arg0.split("=", -3);
                    if(parts.length == 2) {
                        // Check if the value is not specified
                        if( parts[1].isEmpty() ) showUsageAndExit( String.format(Texts.IMsg_ArgParserVarAssignReqArg, parts[0]) );
                        // Add the variable and its value
                        _jmxVarArgs.add( new VarArg( parts[0].trim(), parts[1].trim() ) );
                    }
                    // Target listing
                    else if(parts.length == 1) {
                        // Add the target
                        _jmxTargets.add( parts[0].trim() );
                    }
                    // Invalid argument
                    else {
                        showUsageAndExit( String.format(Texts.IMsg_ArgParserInvalidArgument, arg0) );
                    }
                    // Save the argument
                    _savedArgs.add(arg0);
                    break;

            } // switch arg0

        } // for i

        // Create a temporary file to execute the given program text as needed
        if( sbPrg.length() > 0 ) {
            // Error if both '-e' and '-f' are specified
            if( !_jmxSpecFile.equals(JMX_DefaultSpecFile) ) showUsageAndExit(Texts.IMsg_ArgParserErrorOptEOptF);
            // Set the JxMake specification file
            _jmxSpecFile = SysUtil.resolvePath( "__CMD_LINE_PROGRAM__.jxm", SysUtil.getRootTmpDir() );
            // Set the JxMake start path as needed
            if( _jmxStartPath.equals(JMX_DefaultStartPath) ) _jmxStartPath = SysUtil.getCWD();
            // Save the program text
            Files.write( Paths.get(_jmxSpecFile), sbPrg.toString().getBytes(SysUtil._CharEncoding) );
        }
    }

} // class ArgParser
