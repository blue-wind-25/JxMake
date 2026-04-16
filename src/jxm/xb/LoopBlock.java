/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import jxm.*;


public abstract class LoopBlock extends ContainerBlock {

    protected LoopBlock(final String path, final int lNum, final int cNum, final boolean allocExecBlocksElem)
    { super(path, lNum, cNum, null, allocExecBlocksElem ? 1 : -1); }

    public LoopBlock(final String path, final int lNum, final int cNum)
    { this(path, lNum, cNum, true); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract boolean initLoopCondition(final XCom.ExecData execData) throws JXMException;
    public abstract boolean evalLoopCondition(final XCom.ExecData execData) throws JXMException;
    public abstract boolean updtLoopCondition(final XCom.ExecData execData) throws JXMException;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Initialize the loop condition
        try {
            if( !initLoopCondition(execData) ) return XCom.ExecuteResult.Done;
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }

        // Execute the loop
        XCom.ExecuteResult xres = XCom.ExecuteResult.Done;
        boolean            loop = true;

        while(loop) {

            // Evaluate the loop condition
            try {
                if( !evalLoopCondition(execData) ) break;
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Throw as a different exception
                throw XCom.newJXMRuntimeError( e.toString() );
            }

            // Execute the body
            xres = executeBody(execData, 0);

            switch(xres) {

                case Done:
                    // Check for errors that may have been missed
                    if( isErrorBlockSet() ) {
                        setErrorFromBlock( getErrorBlock() );
                        return XCom.ExecuteResult.Error;
                    }
                    break;

                case Error:
                    setErrorFromBlock( getErrorBlock() );
                    return XCom.ExecuteResult.Error;

                case ProgramExit:
                    return XCom.ExecuteResult.ProgramExit;

                case FunctionReturn:
                    return XCom.ExecuteResult.FunctionReturn;

                case LoopContinue:
                    xres = XCom.ExecuteResult.Done;
                    break;

                case LoopBreak:
                    xres = XCom.ExecuteResult.Done;
                    loop = false;
                    break;

                default:
                    // NOTE : This should never got executed!
                    setErrorFromString(Texts.EMsg_UnknownRuntimeError);
                    return XCom.ExecuteResult.Error;

            } // switch xres

            // Update the loop condition
            try {
                if( !updtLoopCondition(execData) ) break;
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Throw as a different exception
                throw XCom.newJXMRuntimeError( e.toString() );
            }

        } // while true

        // Done
        return xres;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class LoadLoopData extends LoadContainerData {
        public LoadLoopData(final LoadContainerData loadCD)
        { super(loadCD.loadPLC, loadCD.blockName, loadCD.arrayExecBlocks); }
    };

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    { super.saveToStream(dos); }

    public static LoadLoopData loadLDFromStream(final DataInputStream dis) throws Exception
    {
        final ContainerBlock.LoadContainerData loadCD = loadCDFromStream(dis);

        if(loadCD.blockName != null || loadCD.arrayExecBlocks.length != 1) return null;

        return new LoadLoopData(loadCD);
    }

} // class LoopBlock
