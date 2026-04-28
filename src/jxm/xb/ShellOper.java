/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.Pattern;

import jxm.*;


public class ShellOper {

    public static byte SOF_Execute       = 0b00000001;
    public static byte SOF_PrintCommand  = 0b00000010;
    public static byte SOF_SuppressError = 0b00000100;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // https://www.baeldung.com/run-shell-command-in-java
    // https://mkyong.com/java/how-to-execute-shell-command-from-java
    // https://docs.oracle.com/javase/7/docs/api/java/lang/ProcessBuilder.html

    private final ProcessBuilder _processBuilder = new ProcessBuilder();

    private final StringBuilder  _sbStdErr       = new StringBuilder ();
    private final StringBuilder  _sbStdOut       = new StringBuilder ();
    private       int            _exitCode       = 0;

    private       String         _eStr           = null;

    private       boolean        _enableDummyTP  = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class AltGLibC {
        public final String libAbsPath;
        public final String ldsAbsPath;

        public AltGLibC(final String libAbsPath_, final String ldsAbsPath_)
        {
            libAbsPath = libAbsPath_;
            ldsAbsPath = ldsAbsPath_;
        }
    }

    private static final HashMap<String, AltGLibC> _altGLibCMap = new HashMap<>();

    public synchronized static void setAltGLibC(final String exeAbsPath, final String libAbsPath, final String ldsAbsPath)
    {
        if( !SysUtil.osIsLinux() || exeAbsPath.isEmpty() ) return;

        if( libAbsPath.isEmpty() || ldsAbsPath.isEmpty() ) {
            _altGLibCMap.remove(exeAbsPath);
            return;
        }

        _altGLibCMap.put( exeAbsPath, new AltGLibC(libAbsPath, ldsAbsPath) );
    }

    public synchronized static AltGLibC _getAltGLibC(final String exeAbsPath)
    { return SysUtil.osIsLinux() ? _altGLibCMap.get(exeAbsPath) : null; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class CmdInfo {
        public String  path;    // JxMake file path   string
        public int     lNum;    // JxMake file line   number
        public int     cNum;    // JxMake file column number
        public byte    soFlag;  // Shell operation flag

        public CmdInfo(final String path_, final int lNum_, final int cNum_, final byte soFlag_)
        {
            path   = path_;
            lNum   = lNum_;
            cNum   = cNum_;
            soFlag = soFlag_;
        }

        public boolean executeCommand()
        { return (soFlag & SOF_Execute) != 0; }

        public boolean printCommand()
        { return (soFlag & SOF_PrintCommand) != 0; }

        public boolean suppressError()
        { return (soFlag & SOF_SuppressError) != 0; }
    }

    private ArrayList< ArrayList<String> > _commands = new ArrayList<>();
    private ArrayList< CmdInfo           > _cmdInfos = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ShellOper()
    {}

    public ShellOper deepClone()
    {
        final ShellOper newShellOper = new ShellOper();

        newShellOper._sbStdErr.append( _sbStdErr.toString() );
        newShellOper._sbStdOut.append( _sbStdOut.toString() );

        newShellOper._exitCode      = newShellOper._exitCode;
        newShellOper._eStr          = newShellOper._eStr;
        newShellOper._enableDummyTP = newShellOper._enableDummyTP;

        return newShellOper;
    }

    public void setEnableDummyTargetPreq(final boolean enableDummyTP)
    { _enableDummyTP = enableDummyTP; }

    public String getErrorString()
    { return _eStr; }

    public boolean isErrorStringSet()
    { return _eStr != null; }

    public boolean hasPendingOperations()
    { return !_commands.isEmpty(); }

    public void clearState()
    {
        // Clear the error string and exit code
        _eStr     = null;
        _exitCode = 0;

        // Clear the string builders
        _sbStdErr.setLength(0);
        _sbStdOut.setLength(0);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _sprintError(final CmdInfo cmdInfo, final String errMsg)
    { return SysUtil.sprintError(cmdInfo.path, cmdInfo.lNum, cmdInfo.cNum, errMsg); }

    private boolean _checkSpcCmdArgCnt(final ArrayList<String> command, final CmdInfo cmdInfo, final int reqArgCnt)
    {
        if( command.size() != reqArgCnt + 1 ) {
            _eStr = _sprintError( cmdInfo, XCom.errorString( Texts.EMsg_InvalidShellCommandPCnt, command.get(0), reqArgCnt, command.size() - 1 ) );
            return false;
        }

        return true;
    }

    private boolean _checkSpcCmdArgCntAtLeast(final ArrayList<String> command, final CmdInfo cmdInfo, final int reqArgCntAtLeast)
    {
        if( command.size() < reqArgCntAtLeast + 1 ) {
            _eStr = _sprintError( cmdInfo, XCom.errorString( Texts.EMsg_InvalidShellCommandPMC1, command.get(0) ) );
            return false;
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // The 'jxmake' command is excluded from the list of special commands
    private static final Pattern _pmSpecialCommand = Pattern.compile("(?:clr|del|set|add)env|sstdin");

    // Parsing C++ command-line arguments in Windows:
    //     https://learn.microsoft.com/en-us/cpp/cpp/main-function-command-line-args?redirectedfrom=MSDN&view=msvc-170
    private static final Pattern _pmCmdEscMatch1a   = Pattern.compile("(\\\\+(?=\"))"); // Under normal Windows
    private static final Pattern _pmCmdEscMatch1b   = Pattern.compile("(\\\\)"       ); // Under Cygwin & MSys
    private static final String  _pmCmdEscReplace1  = "$1$1";

    private static final Pattern _pmCmdEscMatch2    = Pattern.compile("(\\\")"       );
    private static final String  _pmCmdEscReplace2  = "\\\\$1";

    // The 'jxmake' command is excluded from the list of special commands
    private static final boolean _isSpecialCommand(final String command)
    { return _pmSpecialCommand.matcher(command).matches(); }

    public void addCommand(final ArrayList<XCom.VariableValue> commands, final String path, final int lNum, final int cNum, final byte soFlag) throws JXMRuntimeError
    {
        // Check if the command is empty
        if( commands.get(0) == null || commands.get(0).isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_ShellCmdEvalToEmpty);

        // Store the command and its parameter into a list
        final boolean           osIsWindows    = SysUtil.osIsWindows();
        final boolean           osIsCygwinMSys = SysUtil.osIsCygwin () || SysUtil.osIsMSys ();
        final boolean           isSpecialCommand = _isSpecialCommand( commands.get(0).get(0).value );
        final ArrayList<String> commandList      = new ArrayList<>();

        for(final XCom.VariableValue varVal : commands) {
            for(final XCom.VariableStore varStr : varVal) {
                // Unless this is a special command, skip effectively empty values
                if( !isSpecialCommand && varStr.value.trim().isEmpty() ) continue;
                // If the operating system is Windows, perform some character replacements due to the parsing rules
                if(osIsCygwinMSys) {
                    final String repStr1 = _pmCmdEscMatch1b.matcher(varStr.value).replaceAll(_pmCmdEscReplace1);
                    final String repStr2 = _pmCmdEscMatch2 .matcher(repStr1     ).replaceAll(_pmCmdEscReplace2);
                    commandList.add(repStr2);
                }
                else if(osIsWindows) {
                    final String repStr1 = _pmCmdEscMatch1a.matcher(varStr.value).replaceAll(_pmCmdEscReplace1);
                    final String repStr2 = _pmCmdEscMatch2 .matcher(repStr1     ).replaceAll(_pmCmdEscReplace2);
                    commandList.add(repStr2);
                }
                // Otherwise, store the value as is
                else {
                    commandList.add(varStr.value);
                }
            }
        }

        // Store the command list and information
        _commands.add( commandList                           );
        _cmdInfos.add( new CmdInfo(path, lNum, cNum, soFlag) );
    }

    public boolean executeCommands(final ExecState execState)
    {
        // Check if there is nothing to execute
        if( _commands.isEmpty() ) return true;

        // Clear the state
        clearState();

        // Save the original environment variables
        final HashMap<String, String> originalEnv = new HashMap<>();

        for( final Map.Entry<String, String> item : _processBuilder.environment().entrySet() ) {
            originalEnv.put( item.getKey(), item.getValue() );
        }

        // Set the working directory
        _processBuilder.directory( new File( SysUtil.getCWD() ) );

        // Execute the commands
        boolean execError = false;
        CmdInfo cmdInfo   = null;

        try {

            int               seCnt      = 0;
            ArrayList<String> stdinLines = null;

            for(final ArrayList<String> command : _commands) {

                // Check if error should be suppressed
                cmdInfo = _cmdInfos.get(seCnt++);

                if( cmdInfo.suppressError() ) execState.setLastSupError(null);

                // Print the command and its argument(s) as needed
                if( execState.getCmdEcho() && cmdInfo.printCommand() ) {
                  //for(final String s : command) SysUtil.stdOut().printf(  "%s "  , s);
                    for(final String s : command) SysUtil.stdOut().printf("\"%s\" ", s);
                  //for(final String s : command) SysUtil.stdOut().printf("\'%s\' ", s);
                    SysUtil.stdOut().println();
                }

                // Check if the command shall not actually be executed
                if( !cmdInfo.executeCommand() ) continue;

                // Process special commands
                boolean specialCommand = false;
                boolean jxmakeCommand  = false;

                switch( command.get(0) ) {

                    case "clrenv":
                        // Check the number of arguments
                        if( ( execError = !_checkSpcCmdArgCnt(command, cmdInfo, 0) ) ) break;
                        // Delete all the environment variables
                        _processBuilder.environment().clear();
                        // Set flag
                        specialCommand = true;
                        break;

                    case "delenv":
                        // Check the number of arguments
                        if( ( execError = !_checkSpcCmdArgCnt(command, cmdInfo, 1) ) ) break;
                        // Delete the specified environment variable
                        _processBuilder.environment().remove( command.get(1) );
                        // Set flag
                        specialCommand = true;
                        break;

                    case "setenv":
                        // Check the number of arguments
                        if( ( execError = !_checkSpcCmdArgCnt(command, cmdInfo, 2) ) ) break;
                        // Replace the specified environment variable
                        _processBuilder.environment().remove( command.get(1)                 );
                        _processBuilder.environment().put   ( command.get(1), command.get(2) );
                        // Set flag
                        specialCommand = true;
                        break;

                    case "addenv":
                        // Check the number of arguments
                        if( ( execError = !_checkSpcCmdArgCnt(command, cmdInfo, 2) ) ) break;
                        // Add to the specified environment variable
                        final String orgEnvStr = _processBuilder.environment().get( command.get(1) );
                        final String newEnvStr = (orgEnvStr != null) ? ( orgEnvStr + command.get(2) ) : command.get(2);
                        _processBuilder.environment().remove( command.get(1)            );
                        _processBuilder.environment().put   ( command.get(1), newEnvStr );
                        // Set flag
                        specialCommand = true;
                        break;

                    case "sstdin":
                        // Check the number of arguments
                        if( ( execError = !_checkSpcCmdArgCnt(command, cmdInfo, 1) ) ) break;
                        // Split the lines
                        stdinLines = XCom.explode( command.get(1), "\n" );
                        // Set flag
                        specialCommand = true;
                        break;

                    case "jxmake":
                        // Check the number of arguments
                        if( ( execError = !_checkSpcCmdArgCntAtLeast(command, cmdInfo, 1) ) ) break;
                        // Set flag
                        specialCommand = true;
                        jxmakeCommand  = true;
                        break;

                } // switch

                if(execError) break;
                if(specialCommand && !jxmakeCommand) continue;

                // Modify the command for 'jxmake'
                if(jxmakeCommand) {
                    final ArrayList<String> javaCmd = SysUtil.getJavaCmd();
                    if( javaCmd.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_CannotGetOrgJavaCommand);
                    command.remove(0         );
                    command.addAll(0, javaCmd);
                    /*
                    for(final String s : command) SysUtil.stdDbg().print(s + ' ');
                    SysUtil.stdDbg().println();
                    //*/
                }

                // Check if the executable needs to be executed using an alternative glibc
                final AltGLibC altGLibC            = _getAltGLibC( command.get(0) );
                      String   org_LD_LIBRARY_PATH = null;

                if(altGLibC != null) {
                    // Save the original LD_LIBRARY_PATH
                    org_LD_LIBRARY_PATH = _processBuilder.environment().get("LD_LIBRARY_PATH");
                    // Concatenate the LD_LIBRARY_PATH
                    final String new_LD_LIBRARY_PATH = (org_LD_LIBRARY_PATH != null)
                                                     ? (org_LD_LIBRARY_PATH + ':' + altGLibC.libAbsPath)
                                                     :                              altGLibC.libAbsPath;
                    // Set the environment variable
                    _processBuilder.environment().remove("LD_LIBRARY_PATH"           );
                    _processBuilder.environment().put   ("LD_LIBRARY_PATH", new_LD_LIBRARY_PATH);
                    // Modify the command
                    command.add(0, altGLibC.ldsAbsPath);
                }

                // Set the command and its arguments
                _processBuilder.command(command);

                // Streaming mode
                if( execState.getCmdStreaming() ) {
                    // Inherit IO
                    _processBuilder.inheritIO();
                    // Start the process
                    final Process process = _processBuilder.start();
                    // Get the exit code
                    _exitCode = process.waitFor();
                    // Restore the pipes
                    _processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
                    _processBuilder.redirectError (ProcessBuilder.Redirect.PIPE);
                    _processBuilder.redirectInput (ProcessBuilder.Redirect.PIPE);
                }

                // Whole-block mode
                else {

                    // Start the process
                    final Process process = _processBuilder.start();

                    // Connect the streams
                    final OutputStream       osStdIn    = process.getOutputStream();
                    final InputStream        isStdErr   = process.getErrorStream ();
                    final InputStream        isStdOut   = process.getInputStream ();

                    final OutputStreamWriter oswStdIn   = new OutputStreamWriter(osStdIn , SysUtil._CharEncoding);
                    final InputStreamReader  isrStdErr  = new InputStreamReader (isStdErr, SysUtil._CharEncoding);
                    final InputStreamReader  isrStdOut  = new InputStreamReader (isStdOut, SysUtil._CharEncoding);

                    final BufferedWriter     procStdIn  = new BufferedWriter(oswStdIn , 10 * 1024 * 1024);
                    final BufferedReader     procStdErr = new BufferedReader(isrStdErr, 10 * 1024 * 1024);
                    final BufferedReader     procStdOut = new BufferedReader(isrStdOut, 10 * 1024 * 1024);

                    // Write to from stdin as needed
                    if(stdinLines != null) {
                        for(final String line : stdinLines) {
                            procStdIn.write( line, 0, line.length() );
                            procStdIn.newLine();
                        }
                        procStdIn.flush();
                        procStdIn.close();
                        stdinLines = null;
                    }

                    // Read from stderr and stdout
                    final int noStdXXXChkDly = 100;
                    final int noStdXXXChkMax =  10;
                    final int noStdXXXChkLim =   5;
                          int noStdErrChkCnt =   0;
                          int noStdOutChkCnt =   0;

                    while(true) {

                        // Processing variables
                        boolean gotData = false;
                        String  line    = null;

                        // Read from stderr
                        if( execState.getCmdStdErrChk() && noStdErrChkCnt >= 0 && noStdErrChkCnt < noStdXXXChkMax ) {
                            // Workaround to prevent hung when some program somehow does not have a proper stderr
                            final long startTime = SysUtil.getMS();
                            while(true) {
                                if( procStdErr.ready() ) {
                                    noStdErrChkCnt = -1;
                                    if(noStdOutChkCnt > 0) noStdOutChkCnt = noStdXXXChkMax - (noStdXXXChkLim - noStdOutChkCnt);
                                    break;
                                }
                                if( SysUtil.getMS() - startTime > noStdXXXChkDly ) {
                                    ++noStdErrChkCnt;
                                    break;
                                }
                                Thread.yield();
                            }
                        }
                        if(noStdErrChkCnt <= 0) {
                            while( true || procStdErr.ready() ) {
                                // Read a line from stderr
                                line = procStdErr.readLine();
                                if(line == null) break;
                                _sbStdErr.append( XCom.stripANSIEscapeCodeAsNeeded(line) + "\n" );
                                gotData = true;
                                // Break for now to read from stdout if it becomes ready
                                if( procStdOut.ready() ) break;
                            }
                        }

                        // Read from stdout
                        if( execState.getCmdStdOutChk() && noStdOutChkCnt >= 0 && noStdOutChkCnt < noStdXXXChkMax ) {
                            // Workaround to prevent hung when some program somehow does not have a proper stdout
                            final long startTime = SysUtil.getMS();
                            while(true) {
                                if( procStdOut.ready() ) {
                                    noStdOutChkCnt = -1;
                                    if(noStdErrChkCnt > 0) noStdErrChkCnt = noStdXXXChkMax - (noStdXXXChkLim - noStdErrChkCnt);
                                    break;
                                }
                                if( SysUtil.getMS() - startTime > noStdXXXChkDly ) {
                                    ++noStdOutChkCnt;
                                    break;
                                }
                                Thread.yield();
                            }
                        }
                        if(noStdOutChkCnt <= 0) {
                            while( true || procStdOut.ready() ) {
                                // Read a line from stdout
                                line = procStdOut.readLine();
                                if(line == null) break;
                                _sbStdOut.append( XCom.stripANSIEscapeCodeAsNeeded(line) + "\n" );
                                gotData = true;
                                // Break for now to read from stderr if it becomes ready
                                if( procStdErr.ready() ) break;
                            }
                        }

                        /*
                        if( execState.getCmdStdErrChk() || execState.getCmdStdOutChk() ) {
                            SysUtil.stdDbg().println("### " + noStdErrChkCnt + " " + noStdOutChkCnt);
                        }
                        //*/

                        // Break if no more data can be read
                        if(!gotData) break;

                    } // while true;

                    // Get the exit code
                    _exitCode = process.waitFor();

                    // Close the streams
                    procStdIn .close();
                    procStdErr.close();
                    procStdOut.close();
                }

                // Restore the LD_LIBRARY_PATH as needed
                if(altGLibC != null) {
                                                    _processBuilder.environment().remove("LD_LIBRARY_PATH"                     );
                    if(org_LD_LIBRARY_PATH != null) _processBuilder.environment().put   ("LD_LIBRARY_PATH", org_LD_LIBRARY_PATH);
                }

                // Check if the exit code is not zero
                if(_exitCode != 0) {
                    // Stop further execution
                    break;
                }

            } // for

        } // try
        catch(final Exception e) {
            // Restore state if required
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // If the suppress error flag is not set, raise a normal error
            if( !cmdInfo.suppressError() ) {
              //SysUtil.stdDbg().println("$$$ ERR $$$");
                _eStr     = _sprintError( cmdInfo, e.toString() );
                execError = true;
            }
            // Otherwise, store the actual error message to the execution state so that it can be read by the program later
            else {
              //SysUtil.stdDbg().println("$$$ SUP $$$");
                final String errMsg = e.toString();
                execState.setLastSupError(errMsg);
                SysUtil.printSuppressedError(cmdInfo.path, cmdInfo.lNum, cmdInfo.cNum, errMsg);
            }
        }

        // Clear the commands
        _commands.clear();
        _cmdInfos.clear();

        // Restore the environment variables
        _processBuilder.environment().clear();
        _processBuilder.environment().putAll(originalEnv);

        // Done
        return !execError;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getExitCode(final ExecState execState) throws JXMRuntimeError
    {
        if( hasPendingOperations() ) {
            if( !executeCommands(execState) ) throw XCom.newJXMRuntimeError("read '$[excode]': " + _eStr);
        }

        return String.valueOf(_exitCode);
    }

    public String getStderrText(final ExecState execState) throws JXMRuntimeError
    {
        if( hasPendingOperations() ) {
            if( !executeCommands(execState) ) throw XCom.newJXMRuntimeError("read '$[stderr]': " + _eStr);
        }

        if( _enableDummyTP && _sbStdErr.length() == 0 ) return "STDERR";

        return _sbStdErr.toString();
    }

    public String getStdoutText(final ExecState execState) throws JXMRuntimeError
    {
        if( hasPendingOperations() ) {
            if( !executeCommands(execState) ) throw XCom.newJXMRuntimeError("read '$[stdout]': " + _eStr);
        }

        if( _enableDummyTP && _sbStdOut.length() == 0 ) return "STDOUT";

        return _sbStdOut.toString();
    }

} // class ShellOper
