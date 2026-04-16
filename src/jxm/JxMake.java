/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.nio.file.attribute.FileTime;

import javax.swing.ToolTipManager;

import jxm.xb.*;
import jxm.xb.fci.*;


public class JxMake {

    public static void initializeGlobal()
    {
        /*
         * Re-enable MD5 and SHA-1 as HTTP DIGEST mechanisms that were disabled by:
         *     https://bugs.openjdk.org/browse/JDK-8282649
         */
        System.setProperty("http.auth.digest.reEnabledAlgorithms", "MD5, SHA-1");

        // Install Jansi to the system
        SysUtil.jansiSystemInstall();

        // Initialize serial port
        SerialPortUtil.initialize();

        // Make tooltips show faster and stay longer
        ToolTipManager.sharedInstance().setInitialDelay(  100);
        ToolTipManager.sharedInstance().setReshowDelay (    0);
        ToolTipManager.sharedInstance().setDismissDelay(25000);
    }

    public static void process(final String[] args)
    {
        try {

            // Parse the argument(s)
            final ArgParser argParser = new ArgParser(args);

            // Enable hardware headless mode if asked
            if( argParser.enableHeadless() ) System.setProperty("java.awt.headless", "true");

            // Enable printing all exception stack trace on error if asked
            if( argParser.enableAllExceptionStackTrace() ) XCom.setEnableAllExceptionStackTrace(true);

            // Enable hardware acceleration via OpenGL or Direct3D if asked
            if( argParser.enableOpenGL  () ) System.setProperty("sun.java2d.opengl", "true");
            if( argParser.enableDirect3D() ) System.setProperty("sun.java2d.d3d"   , "true");

            // Initialize global
            initializeGlobal();

            /*
            // Save the current working directory
            final String cwdSaved = SysUtil.getCWD();
            */

            // Determine the absolute paths and change directory
            String jxmSpecFile_absPath = SysUtil.resolveAbsolutePath( argParser.jxmSpecFile() );

            if( !SysUtil.pathIsValidFile(jxmSpecFile_absPath) ) {
                SysUtil.setCWD( argParser.jxmStartPath() );
                jxmSpecFile_absPath = SysUtil.resolveAbsolutePath( argParser.jxmSpecFile() );
            }
            else {
                final String startPath_absDirPath   = SysUtil.getDirName( SysUtil.resolveAbsolutePath( argParser.jxmStartPath() ) );
                final String jxmSpecFile_absDirPath = SysUtil.getDirName( jxmSpecFile_absPath                                     );
                if( argParser.jxmStartPath().equals(ArgParser.JMX_DefaultStartPath) && !startPath_absDirPath.equals(jxmSpecFile_absDirPath) ) {
                    // If the start path is *still the default* start path, use the directory of the specification file as the start path
                    SysUtil.setCWD(jxmSpecFile_absDirPath);
                }
            }

            /*
            // Restart the application as needed
            if( !cwdSaved.equals( SysUtil.getCWD() ) ) {
                if(false) {
                    SysUtil.stdOut().println("### " + cwdSaved + " ###");
                    for(final String s : argParser.savedArgs() ) SysUtil.stdOut().println(s);
                    SysUtil.systemExit(0);
                }
                if( !SysUtil.restartApplication( SysUtil.getCWD(), argParser.savedArgs() ) ) {
                    SysUtil.stdOut().println(Texts.EMsg_AppRestartError);
                    SysUtil.systemExitError();
                }
            }
            //*/

            /*
            for( final String s : SysUtil.getJavaCmd() ) SysUtil.stdDbg().println(s);
            SysUtil.systemExit();
            //*/

            // Initialize the color theme selection
            XCom.initColorSelection( argParser.useLightColorTheme() );

            // Silence all suppressed error messages if asked
            if( argParser.silentSuppressedError() ) SysUtil.setSilentSErrMsg(true);

            // Disable the actual output of ANSI escape codes if asked
            if( argParser.disableANSIEscapeCode() ) XCom.setDisableANSIEscapeCode(true);

            // Prepare the project temporary directory
            if( !SysUtil.setSavedProjectTmpDir(jxmSpecFile_absPath) ) throw XCom.newJXMRuntimeError("cannot prepare the project temporary directory");

            // Run the documentation browser and exit if the user requests it
            if( argParser.runDocumentBrowser() ) {
                ( new DocBrowser( !argParser.useLightColorThemeGUI() ) ).run();
                SysUtil.systemExit();
            }

            // Run the script editor and exit if the user requests it
            if( argParser.runScriptEditor() ) {
                final String initialFilePath = SysUtil.pathIsValidFile(jxmSpecFile_absPath) ? argParser.jxmSpecFile() : null;
                ( new ScriptEditor( !argParser.useLightColorThemeGUI(), initialFilePath ) ).run();
                SysUtil.systemExit();
            }

            // Enable warning when evaluating reference variables which contain invalid references if asked
            if( argParser.enableWarnEvalInvRefVar() ) Option.OptionStack.setGlobalEnableWarnEvalInvRefVar(true);

            // Enable warning when evaluating non-existent variables if asked
            if( argParser.enableWarnEvalVarNotExist() ) Option.OptionStack.setGlobalEnableWarnEvalVarNotExist(true);

            // Enable warning when converting strings to integers if asked
            if( argParser.enableWarnCnvStringInteger() ) Option.OptionStack.setGlobalEnableWarnCnvStringInteger(true);

            // Enable warning when converting strings to booleans if asked
            if( argParser.enableWarnCnvStringBoolean() ) Option.OptionStack.setGlobalEnableWarnCnvStringBoolean(true);

            // Delete the compiled JxMake specification file cache and exit?
            if( argParser.deleteJXMSpecFileCacheAndExit() ) {
                XSaver.deleteCacheFile(jxmSpecFile_absPath, true);
                SysUtil.systemExit();
            }

            // Delete all the project's cache files and exit?
            if( argParser.deleteProjectFileCacheAndExit() ) {
                SysUtil.delProjectTmpDir(jxmSpecFile_absPath, true);
                SysUtil.systemExit();
            }

            // Extract the embedded JxMake loadable library and/or documentation  files and exit
            if( argParser.extractEJARLibAndExit() || argParser.extractEJARDocsAndExit() || argParser.extractEJARABrdDecAndExit() ) {
                // ##### !!! TODO : Allow extract to a different directory? !!! #####
                if( argParser.extractEJARDocsAndExit   () ) SysUtil.copyJARRes_embeddedDoc    (null, true);
                if( argParser.extractEJARLibAndExit    () ) SysUtil.copyJARRes_embeddedLib    (null, true);
                if( argParser.extractEJARABrdDecAndExit() ) SysUtil.copyJARRes_embeddedABrdDec(null, true);
                SysUtil.systemExit();
            }

            // Variables to store the compilation result
            String          mainJXMSpecFile_absPath = null;
            XCom.ExecBlocks mainExecBlocks          = null;

            // Try to load from the compiled JxMake specification file cache
            FileTime latestFileTime = null;

            if( argParser.enableJXMSpecFileCache() ) {
                // Set verbose mode
                XCacheHelper.setVerboseMode( argParser.enableJXMSpecFileCacheVerbose() );
                // Try to load from the cache
                mainExecBlocks = XLoader.loadFromCacheFile(jxmSpecFile_absPath);
                if(mainExecBlocks != null) {
                    mainJXMSpecFile_absPath = jxmSpecFile_absPath;
                    latestFileTime          = SysUtil.pathGetTime(jxmSpecFile_absPath);
                }
            }

            // Compile and save the specification file
            if(mainExecBlocks == null) {
                // Compile the specification file
                final XBBuilder  xbBuilder  = new XBBuilder ( jxmSpecFile_absPath                 );
                final SpecReader specReader = new SpecReader( argParser.enableJXMLibLoadVerbose() );

                xbBuilder.addMacroDefs( specReader.pushSpecFile( xbBuilder.mainJXMSpecFile_absPath(), false, false ) );

                while(true) {

                    // Get the token and check for EOF
                    final TokenReader.Token token = specReader.readToken(xbBuilder);

                    if( token == null && !specReader.isLibBinFileLoaded() ) break;

                    // If a compiled library file has been loaded, append its execution blocks to the builder
                    if( specReader.isLibBinFileLoaded() ) {
                        xbBuilder.storeLibExecBlocks( specReader.getLibBinFileExecBlocks() );
                        continue;
                    }

                    // Process the token and break on error
                    if( !xbBuilder.processToken(specReader, token) ) break;

                } // while true

                // Check for error
                if( !xbBuilder.done() ) {
                    xbBuilder.errorToken().printError();
                    SysUtil.systemExitError();
                }

                // Store the compilation result
                mainJXMSpecFile_absPath = xbBuilder.mainJXMSpecFile_absPath();
                mainExecBlocks          = xbBuilder.execBlocks();

                // Delete all the project's cache files and exit?
                if( argParser.compileJMXSpecBinFileAndExit() ) {
                    final boolean res = XSaver.saveToLibraryFile( mainJXMSpecFile_absPath + SysUtil._JxMakeProgramBinFileExt, mainExecBlocks, xbBuilder.macroDefs() );
                    if(!res) SysUtil.systemExitError();
                    else     SysUtil.systemExit();
                }

                // Try to save the compiled cache
                if( argParser.enableJXMSpecFileCache() ) {
                    XSaver.saveToCacheFile( mainJXMSpecFile_absPath, specReader.getAllSpecFileList(), mainExecBlocks );
                }

                // Save the latest file time
                latestFileTime = specReader.getLatestFileTime();
            }

            // Execute the program if the compilation process was completed successfully
            if(mainExecBlocks != null) {

                // Prepare the execution
                final XBExec xbExecutor = new XBExec(latestFileTime);

                // Enable dummy target and prerequisites as needed
                xbExecutor.getExecState().setEnableDummyTargetPreq( argParser.enableDummyTargetPreq() );

                // Set the JxMake specification file
                xbExecutor.getExecState().setJxMakeFile(mainJXMSpecFile_absPath);

                // Store the variable(s) defined from command line
                for( final ArgParser.VarArg varArg : argParser.jmxVarArgs() ) {
                    xbExecutor.getExecState().setGlobalVar( XCom.genRVarName(varArg.name), new XCom.VariableStore(true, varArg.value), false, false );
                }

                // Execute the program
                if( !xbExecutor.execute( mainJXMSpecFile_absPath, mainExecBlocks, argParser.jmxTargets() ) ) xbExecutor.printError();

                // Exit with the exit code
                SysManip.__force__execute_sh_restore__on_program_exit__();
                SysUtil.systemExit( xbExecutor.getExitCode() );

            } // if

        } // try
        catch(final Exception e) {
            // If we got here, print the stack trace unconditionally and exit
            e.printStackTrace();
            SysManip.__force__execute_sh_restore__on_program_exit__();
            SysUtil.systemExitError();
        }

        // Something went wrong if it got here!
        XCom.newJXMFatalLogicError("The program flow should never get here!").printStackTrace();
        SysManip.__force__execute_sh_restore__on_program_exit__();
        SysUtil.systemExitError();
    }

} // class JxMake
