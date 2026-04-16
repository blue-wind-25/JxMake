/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.ArrayList;

import java.util.concurrent.Callable;

import jxm.*;


public class TargetRun extends ExecBlock {

    private final XCom.ReadVarSpecs _rvarSpecsT;
    private final boolean           _useThread;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public TargetRun(final String path, final int lNum, final int cNum, final XCom.ReadVarSpecs rvarSpecsT, final boolean useThread)
    {
        super(path, lNum, cNum);

        _rvarSpecsT = rvarSpecsT;
        _useThread  = useThread;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private XCom.ExecuteResult _execute_impl(final XCom.ExecData execData, final TargetRun targetRun) throws JXMException
    {
        // Evaluate the argument(s)
        final ArrayList<String> targets = new ArrayList<>();

        for(final XCom.ReadVarSpec item : targetRun._rvarSpecsT) {
            for( final XCom.VariableStore varStr : execData.execState.readVar(this, execData, item, true) ) {
                final String targetName = varStr.value.trim();
                if( !targetName.isEmpty() ) targets.add(targetName);
            }
        }

        // Execute the targets
        final Target.ExecTargetResult etr = Target.execTargets( targetRun.getPath(), execData, null, targets );

        // Check for error
        if(etr.errExecBlock != null) {
            if(etr.errExecBlock instanceof Target) {
                final Target target = (Target) etr.errExecBlock;
                     if( target.isErrorBlockSet () ) targetRun.setErrorFromBlock ( target                                          );
                else if( target.isErrorStringSet() ) targetRun.setErrorFromString( target.getErrorString()                         );
                else                                 targetRun.setErrorFromString( XCom.errorString(Texts.EMsg_UnknownRuntimeError) );
            }
            else {
                if( etr.errExecBlock.isErrorStringSet() ) targetRun.setErrorFromString( etr.errExecBlock.getErrorString()               );
                else                                      targetRun.setErrorFromString( XCom.errorString(Texts.EMsg_UnknownRuntimeError) );
            }
            return XCom.ExecuteResult.Error;
        }

        // Done
        return etr.executeResult;
    }

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // ===== Execute using the current thread =====
        if(!_useThread) {
            final XCom.ExecuteResult xres = _execute_impl(execData, this);
            return xres;
        }

        // ===== Execute using a new thread =====
        // Error if this thread is not the main thread
        if(execData.inThread) {
            setErrorFromString(Texts.EMsg_NestedPJxMake);
            return XCom.ExecuteResult.Error;
        }

        // Save a reference to this instance
        final TargetRun targetRun = this;

        // Submit a new task
        TargetWait._submitTask( new Callable<Void>() {
            @Override
            public Void call()
            {
                // Clone the instances
                final XCom.ExecData clonedExecData  =             execData .deepClone(true);
                final TargetRun     clonedTargetRun = (TargetRun) targetRun.deepClone(    );

                // Execute the targets
                XCom.ExecuteResult xres = XCom.ExecuteResult.Done;

                try {
                    xres = _execute_impl(clonedExecData, clonedTargetRun);
                }
                catch(final JXMException e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Set the error message
                    clonedTargetRun.setErrorFromString( e.toString() );
                    xres = XCom.ExecuteResult.Error;
                }

                // Check for error
                switch(xres) {

                    case Done:
                        /* FALLTHROUGH */
                    case Done_NoUpdate:
                        break;

                    case Error:
                        clonedTargetRun.printError();
                        SysUtil.systemExitError();
                        break;

                    case SuppressedError:
                        clonedTargetRun.printSuppressedError();
                        break;

                    case ProgramExit:
                        SysUtil.systemExit( clonedExecData.execState.getExitCode() );
                        break;

                    default:
                        // NOTE : This should never got executed!
                        clonedTargetRun.setErrorFromString( XCom.errorString(Texts.EMsg_UnknownRuntimeError) );
                        clonedTargetRun.printError();
                        SysUtil.systemExitError();
                        break;

                } // switch

                // Done
                return null;
            }
        } );

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveReadVarSpecs(dos, _rvarSpecsT);

        dos.writeBoolean(_useThread);
    }

    public static TargetRun loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new TargetRun(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadReadVarSpecs(dis),
            dis.readBoolean()
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class TargetRun
