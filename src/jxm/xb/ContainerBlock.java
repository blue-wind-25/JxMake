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


public abstract class ContainerBlock extends ExecBlock {

    /*
     * NOTE : Any class that is a subclass of this 'ContainerBlock' class may not be thread safe because
     *        its member execution blocks may not be thread safe.
     */

    private String             _blockName       = null;
    private XCom.ExecBlocks[]  _arrayExecBlocks = null;
    private XCom.VariableValue _retVal          = null;

    private ExecBlock          _errorBlock      = null;
    private GoTo               _lastGoTo        = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ContainerBlock(final String path, final int lNum, final int cNum, final String blockName, final int cntExecBlocksElem)
    {
        super(path, lNum, cNum);

        _blockName       = blockName;
        _arrayExecBlocks = new XCom.ExecBlocks[ (cntExecBlocksElem < 0) ? -cntExecBlocksElem : cntExecBlocksElem ];

        for(int i = 0; i < cntExecBlocksElem; ++i) {
            _arrayExecBlocks[i] = new XCom.ExecBlocks();
        }
    }

    public ContainerBlock(final String path, final int lNum, final int cNum, int cntExecBlocksElem)
    { this(path, lNum, cNum, "", cntExecBlocksElem); }

    public String setBlockName(final String blockName)
    { return _blockName = blockName; }

    public String getBlockName()
    { return _blockName; }

    public void setExecBlocks(int index, final XCom.ExecBlocks execBlocks)
    { _arrayExecBlocks[index] = execBlocks; }

    public XCom.ExecBlocks getExecBlocks(int index)
    { return _arrayExecBlocks[index]; }

    public XCom.ExecBlocks getExecBlocks()
    { return _arrayExecBlocks[0]; }

    public XCom.VariableValue getReturnValue()
    { return _retVal;}

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setErrorBlock(final ExecBlock errorBlock)
    { _errorBlock = errorBlock; }

    public ExecBlock getErrorBlock()
    { return _errorBlock; }

    public boolean isErrorBlockSet()
    { return _errorBlock != null; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String _evalBoolStr(final String str)
    {
        switch(str) {
            case "true"  : return "1";
            case "false" : return "0";
            default      : return str;
        }
    }

    public boolean evalCmpCondition(final XCom.ExecData execData, final XCom.ReadVarSpec rvarSpec1, final XCom.CompareType cmpType, final XCom.ReadVarSpec rvarSpec2) throws JXMException
    {
        // Evaluate the argument(s)
        final String evalStr1     = XCom.flatten( execData.execState.readVar(this, execData, rvarSpec1, true), "" );
        final String evalStr2     = XCom.flatten( execData.execState.readVar(this, execData, rvarSpec2, true), "" );

        final String evalStr1Bool = _evalBoolStr(evalStr1);
        final String evalStr2Bool = _evalBoolStr(evalStr2);

        // Perform the comparison
        boolean cmpRes = false;

        switch(cmpType) {

            case eq_str  : /* FALLTHROUGH */
            case neq_str : cmpRes = evalStr1.equals(evalStr2);
                           if(cmpType == XCom.CompareType.neq_str) cmpRes = !cmpRes;
                           break;

            case eq      : /* FALLTHROUGH */
            case neq     : cmpRes = ( Long.compare( XCom.toLong(this, execData, evalStr1Bool), XCom.toLong(this, execData, evalStr2Bool) ) == 0 );
                           if(cmpType == XCom.CompareType.neq) cmpRes = !cmpRes;
                           break;

            case lt      : cmpRes = ( Long.compare( XCom.toLong(this, execData, evalStr1Bool), XCom.toLong(this, execData, evalStr2Bool) ) <  0 ); break;
            case lte     : cmpRes = ( Long.compare( XCom.toLong(this, execData, evalStr1Bool), XCom.toLong(this, execData, evalStr2Bool) ) <= 0 ); break;
            case gt      : cmpRes = ( Long.compare( XCom.toLong(this, execData, evalStr1Bool), XCom.toLong(this, execData, evalStr2Bool) ) >  0 ); break;
            case gte     : cmpRes = ( Long.compare( XCom.toLong(this, execData, evalStr1Bool), XCom.toLong(this, execData, evalStr2Bool) ) >= 0 ); break;

        } // switch

        // Return the result
        return cmpRes;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XCom.ExecuteResult executeBody(final XCom.ExecData execData, final int blockIndex)
    {
        // Clear the error block and return value
        setErrorBlock(null);
        _retVal = null;

        // A transient variable to hold a reference to the execution block instance to be executed
        // immediately after this (it is required in case an exception occurs)
        ExecBlock curExecBlock = null;

        // Execute the execution-blocks
        try {

            // Loop through the execution-blocks
            XCom.ExecBlocks execBlocks = getExecBlocks(blockIndex);

            for( int idx = 0; idx < execBlocks.size(); ++idx ) {

                // Get the execution block instance
                final ExecBlock item = execBlocks.get(idx);

                // Save a reference to the execution block instance to be executed immediately after this
                curExecBlock = item;

                // Execute the execution block
                switch( curExecBlock.execute(execData) ) {

                    case Done:
                        break;

                    case Error:
                        setErrorBlock(curExecBlock);
                        return XCom.ExecuteResult.Error;

                    case SuppressedError:
                        curExecBlock.printSuppressedError();
                        break;

                    case ProgramExit:
                        return XCom.ExecuteResult.ProgramExit;

                    case FunctionReturn:
                        if(curExecBlock instanceof ContainerBlock) _retVal = ( (ContainerBlock) curExecBlock ).getReturnValue();
                        else                                       _retVal = ( (Return        ) curExecBlock ).getReturnValue();
                        return XCom.ExecuteResult.FunctionReturn;

                    case LoopContinue:
                        return XCom.ExecuteResult.LoopContinue;

                    case LoopBreak:
                        return XCom.ExecuteResult.LoopBreak;

                    case GoTo:
                        if(this instanceof XCom.LabelMapOwner) {
                            // Get the go-to instance
                            GoTo xbGoTo = null;
                            if(curExecBlock instanceof GoTo) {
                                xbGoTo = (GoTo) curExecBlock;
                            }
                            else {
                                final ContainerBlock contBlock = (ContainerBlock) curExecBlock;
                                xbGoTo              = contBlock._lastGoTo;
                                contBlock._lastGoTo = null;
                            }
                            // Get the go-to index
                            final String  labelStr = xbGoTo.getLabel  ();
                            final boolean safeMode = xbGoTo.isSafeMode();
                            final int     newIdx   = ( (XCom.LabelMapOwner) this ).getLabel(labelStr) - 1;
                            // Check if the label was not found
                            if(newIdx < 0) {
                                // Error if not it is not in safe mode
                                if(!safeMode) throw XCom.newJXMRuntimeError(Texts.EMsg_LabelNotExist, labelStr);
                            }
                            // The label was found
                            else {
                                // Change the index
                                idx = newIdx;
                            }
                        }
                        else {
                            // Propagate the go-to instance up
                            if(curExecBlock instanceof GoTo) {
                                _lastGoTo = (GoTo) curExecBlock;
                            }
                            else {
                                _lastGoTo = ( (ContainerBlock) curExecBlock )._lastGoTo;
                            }
                            return XCom.ExecuteResult.GoTo;
                        }
                        break;

                    default:
                        // NOTE : This should never got executed!
                        setErrorFromString(Texts.EMsg_UnknownRuntimeError);
                        return XCom.ExecuteResult.Error;

                } // switch

            } // for

        } // try
        catch(final JXMException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set the error message and block
            setErrorBlock(curExecBlock);
            if( !getErrorBlock().isErrorStringSet() ) getErrorBlock().setErrorFromString( e.toString() );
            return XCom.ExecuteResult.Error;
        }

        // Done
        return XCom.ExecuteResult.Done;
    }

    // The function 'executeBody()' raises errors by using 'setErrorBlock()' instead of throwing exceptions
    public XCom.ExecuteResult executeBody(final XCom.ExecData execData)
    { return executeBody(execData, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class LoadContainerData {
        public final ExecBlock.LoadPLC loadPLC;
        public final String            blockName;
        public final XCom.ExecBlocks[] arrayExecBlocks;

        public LoadContainerData(final ExecBlock.LoadPLC loadPLC_, final String blockName_, final int cntExecBlocks_)
        {
            loadPLC         = loadPLC_;
            blockName       = blockName_;
            arrayExecBlocks = new XCom.ExecBlocks[cntExecBlocks_];
        }

        public LoadContainerData(final ExecBlock.LoadPLC loadPLC_, final String blockName_, XCom.ExecBlocks[] arrayExecBlocks_)
        {
            loadPLC         = loadPLC_;
            blockName       = blockName_;
            arrayExecBlocks = arrayExecBlocks_;
        }
    };

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveString(dos, _blockName);

        dos.writeInt(_arrayExecBlocks.length);

        for(final XCom.ExecBlocks execBlocks : _arrayExecBlocks) XCacheHelper.saveExecBlocks(dos, execBlocks);
    }

    public static LoadContainerData loadCDFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        final String blockName     = XLoader.loadString(dis);
        final int    cntExecBlocks = dis.readInt();

        final LoadContainerData loadCD = new LoadContainerData(loadPLC, blockName, cntExecBlocks);

        for(int i = 0; i < cntExecBlocks; ++i) loadCD.arrayExecBlocks[i] = XCacheHelper.loadExecBlocks(dis);

        return loadCD;
    }

} // class ContainerBlock
