/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;

import java.text.SimpleDateFormat;

import jxm.*;
import jxm.xb.*;


public class SysManip {

    private static       long    _shOwnerThread = -1;
    private static       boolean _shIsRunning   = false;
    private static       boolean _shLoopCanExit = false;

    private static final Thread  _shutdownHook  = new Thread( new Runnable() {
        @Override
        public void run()
        {
            // Set the 'shutdown-hook-is-running' flag
            _shIsRunning = true;

            // Loop until the 'shutdown-hook-loop-can-exit' flag is set
            while(!_shLoopCanExit) {
                Thread.yield();
                SysUtil.sleepMS(1000);
            }
        }
    } );

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_alt_glibc_for(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get largest number of set members
        final int exeAbsPathCnt = evalVals.get(0).size();
        final int libAbsPathCnt = evalVals.get(1).size();
        final int ldsAbsPathCnt = evalVals.get(2).size();
        final int maxCnt        = Math.max( exeAbsPathCnt, Math.max(libAbsPathCnt, ldsAbsPathCnt) );

        // Set directives
        for(int i = 0; i < maxCnt; ++i) {

            final String exeAbsPath = (i < exeAbsPathCnt) ? evalVals.get(0).get(i).value : evalVals.get(0).get(exeAbsPathCnt - 1).value;
            final String libAbsPath = (i < libAbsPathCnt) ? evalVals.get(1).get(i).value : evalVals.get(1).get(libAbsPathCnt - 1).value;
            final String ldsAbsPath = (i < ldsAbsPathCnt) ? evalVals.get(2).get(i).value : evalVals.get(2).get(ldsAbsPathCnt - 1).value;

            if( !SysUtil.pathIsValid(exeAbsPath) ) throw XCom.newJXMRuntimeError(Texts.EMsg_AltGLibCInvalidPath, "<abs_program_path>");
            if( !SysUtil.pathIsValid(libAbsPath) ) throw XCom.newJXMRuntimeError(Texts.EMsg_AltGLibCInvalidPath, "<abs_lib_path>"    );
            if( !SysUtil.pathIsValid(ldsAbsPath) ) throw XCom.newJXMRuntimeError(Texts.EMsg_AltGLibCInvalidPath, "<abs_ld_path>"    );

            ShellOper.setAltGLibC( exeAbsPath.trim(), libAbsPath.trim(), ldsAbsPath.trim() );

        } // for
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("deprecation")
    public synchronized static void _execute_sh_delay()
    {
        // Exit if the shutdown hook is already added
        if(_shOwnerThread >= 0) return;

        // Add the shutdown hook
        Runtime.getRuntime().addShutdownHook(_shutdownHook);

        // Save the owner thread ID
        _shOwnerThread = Thread.currentThread().getId(); // @@@ Use 'threadId()' when JxMake requires Java SDK 19 or later
    }

    @SuppressWarnings("deprecation")
    public synchronized static void _execute_sh_restore()
    {
        // Exit if the shutdown hook has not been added
        if(_shOwnerThread < 0) return;

        // Exit if the thread is not the same with the one adding the shutdown hook
        if( _shOwnerThread != Thread.currentThread().getId() ) return; // @@@ Use 'threadId()' when JxMake requires Java SDK 19 or later

        // If the shutdown hook is already running, simply set the 'shutdown-hook-loop-can-exit' flag
        if(_shIsRunning) {
            _shLoopCanExit = true;
            return;
        }

        // Remove the shutdown hook
        Runtime.getRuntime().removeShutdownHook(_shutdownHook);

        // Clear the owner thread ID
        _shOwnerThread = -1;
    }

    @SuppressWarnings("deprecation")
    public static void __force__execute_sh_restore__on_program_exit__()
    {
        // NOTE : This function will only be called by the 'JxMake' class on program exit; just in case
        //        the user does not call '$sh_restore' after calling '$sh_delay.

        // Set the 'shutdown-hook-loop-can-exit' flag unconditionally
        _shLoopCanExit = true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_cmd_echo(final XCom.ExecData execData, final boolean echoOn)
    { execData.execState.setCmdEcho(echoOn); }

    public static void _execute_cmd_streaming(final XCom.ExecData execData, final boolean streamingOn)
    { execData.execState.setCmdStreaming(streamingOn); }

    public static void _execute_cmd_stderrchk(final XCom.ExecData execData, final boolean chkOn)
    { execData.execState.setCmdStdErrChk(chkOn); }

    public static void _execute_cmd_stdoutchk(final XCom.ExecData execData, final boolean chkOn)
    { execData.execState.setCmdStdOutChk(chkOn); }

    public static void _execute_cmd_clr_state(final XCom.ExecData execData)
    { execData.execState.getShellOper().clearState(); }

    public static void _execute_silent_SEM()
    { SysUtil.forceSilentSErrMsg(); }

    public static void _execute_restore_SEM()
    { SysUtil.restoreSilentSErrMsg(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_micros(final XCom.VariableValue retVal) throws JXMException
    { retVal.add( new XCom.VariableStore( true, String.valueOf( SysUtil.getUS() ) ) ); }

    public static void _execute_millis(final XCom.VariableValue retVal) throws JXMException
    { retVal.add( new XCom.VariableStore( true, String.valueOf( SysUtil.getMS() ) ) ); }

    public static void _execute_datetime(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        final String format = FuncCall._readFlattenOptParam(evalVals, 0, "yyyy/MM/dd HH:mm:ss");

        retVal.add( new XCom.VariableStore( true, ( new SimpleDateFormat(format) ).format( new Date() ) ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_sleep(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        SysUtil.sleepMS( XCom.toLong( execBlock, execData, XCom.flatten( evalVals.get(0), "" ) ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_getenv(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        final XCom.VariableValue envNames   = evalVals.get(0);
        final XCom.VariableValue defVals    = FuncCall._getOptParam(evalVals, 1);

        final int                evnNamesCnt =                         envNames.size();
        final int                defValsCnt  = (defVals == null) ? 0 : defVals .size();

        if(defValsCnt > evnNamesCnt) throw XCom.newJXMRuntimeError( Texts.EMsg_getenv_NumDefValLarger, defValsCnt, evnNamesCnt);

        for(int i = 0; i < evnNamesCnt; ++i) {
            final String envName = envNames.get(i).value.trim();
            if( !envName.isEmpty() ) {
                final String envVal = SysUtil.getEnv(envName);
                final String defVal = (i < defValsCnt) ? defVals.get(i).value : "null";
                retVal.add( new XCom.VariableStore( true, (envVal != null) ? envVal : defVal ) );
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_clear_project(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        try {
            if( !SysUtil.clrProjectCacheDir( XCom.toBoolean( execBlock, execData, XCom.flatten( evalVals.get(0), "" ) ) ) ) throw XCom.newJXMRuntimeError(Texts.EMsg_NoProjectTmpDir);
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_exit(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    { execData.execState.setExitCode( XCom.toLong( execBlock, execData, XCom.flatten( evalVals.get(0), "" ) ).intValue() ); }

} // class SysManip
