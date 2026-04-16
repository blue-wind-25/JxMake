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


public class VarAssign extends ExecBlock {

    private final String            _varName;
    private final boolean           _local;
    private final boolean           _const;
    private final XCom.ASNSpec      _asnSpec;
    private final FuncCall          _funcCall;
    private final XCom.ReadVarSpecs _rvarSpecs;

    private final boolean           _isPreTargetExec;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public VarAssign(
        final String            path,
        final int               lNum,
        final int               cNum,
        final String            varName,
        final boolean           local,
        final boolean           const_,
        final XCom.ASNSpec      asnSpec,
        final XCom.FuncSpec     funcSpec,
        final XCom.ReadVarSpecs rvarSpecs
    ) {
        super(path, lNum, cNum);

        _varName = varName;
        _local   = local;
        _const   = const_;
        _asnSpec = asnSpec;

        if(funcSpec != null) {
            _funcCall  = new FuncCall(path, lNum, cNum, funcSpec, rvarSpecs);
            _rvarSpecs = null;
        }
        else {
            _funcCall  = null;
            _rvarSpecs = rvarSpecs;
        }

        _isPreTargetExec = XCom.isPreTargetExecSVarSCut(varName);
    }

    public boolean isPreTargetExec()
    { return _isPreTargetExec; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Check if it is a function call
        if(_funcCall != null) {

            // Ensure it is a direct assignment
            if(!_asnSpec.direct) throw XCom.newJXMFatalLogicError(Texts.EMsg_InvalidAssignFLazy); // NOTE : This should never got executed!

            // Execute the function and check the result
            switch( _funcCall.execute(execData) ) {

                case Done:
                    break;

                case Error:
                    setErrorFromBlock(_funcCall);
                    return XCom.ExecuteResult.Error;

                case SuppressedError:
                    _funcCall.printSuppressedError();
                    break;

                case ProgramExit:
                    return XCom.ExecuteResult.ProgramExit;

                default:
                    // NOTE : This should never got executed!
                    setErrorFromString(Texts.EMsg_UnknownRuntimeError);
                    return XCom.ExecuteResult.Error;

            } // switch

            // Get the result
            final XCom.VariableValue retVal = _funcCall.getReturnValue();
            if(retVal == null) throw XCom.newJXMFatalLogicError( Texts.EMsg_InvalidAssignFRet, _funcCall.funcName() ); // NOTE : This should never got executed!

            // Generate the result
            final XCom.ReadVarSpecs rvs = new XCom.ReadVarSpecs();
            for(final XCom.VariableStore item : retVal) {
                rvs.add( new XCom.ReadVarSpec(item.constant, null, item.value, null, null) );
            }

            // Store the result
                 if(_asnSpec.ifNotSet) execData.execState.assignVarIfNotSet(this, execData, _varName, _local,         rvs, true);
            else if(_asnSpec.concat  ) execData.execState.assignVarConcat  (this, execData, _varName, _local,         rvs, true);
            else                       execData.execState.assignVar        (this, execData, _varName, _local, _const, rvs, true);

        }

        // Other variable assignments
        else {
                 if(_asnSpec.ifNotSet) execData.execState.assignVarIfNotSet(this, execData, _varName, _local,         _rvarSpecs, _asnSpec.direct);
            else if(_asnSpec.concat  ) execData.execState.assignVarConcat  (this, execData, _varName, _local,         _rvarSpecs, _asnSpec.direct);
            else                       execData.execState.assignVar        (this, execData, _varName, _local, _const, _rvarSpecs, _asnSpec.direct);
        }

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private VarAssign(
        final String            path,
        final int               lNum,
        final int               cNum,
        final String            varName,
        final boolean           local,
        final boolean           const_,
        final XCom.ASNSpec      asnSpec,
        final FuncCall          funcCall,
        final XCom.ReadVarSpecs rvarSpecs,
        final boolean           isPreTargetExec
    ) {
        super(path, lNum, cNum);

        _varName         = varName;
        _local           = local;
        _const           = const_;
        _asnSpec         = asnSpec;
        _funcCall        = funcCall;
        _rvarSpecs       = XCom.substEmptyReadVarSpecs(rvarSpecs);

        _isPreTargetExec = isPreTargetExec;
    }

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveString(dos, _varName);

        dos.writeBoolean(_local);

        dos.writeBoolean(_const);

        XSaver.saveASNSpec(dos, _asnSpec);

        if( !XCacheHelper.saveIfNull(dos, _funcCall ) ) _funcCall.saveToStream(dos);

        if( !XCacheHelper.saveIfNull(dos, _rvarSpecs) ) XSaver.saveReadVarSpecs(dos, _rvarSpecs);

        dos.writeBoolean(_isPreTargetExec);
    }

    public static VarAssign loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new VarAssign(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadString(dis),
            dis.readBoolean(),
            dis.readBoolean(),
            XLoader.loadASNSpec(dis),
            XCacheHelper.loadIfNull(dis) ? null : FuncCall.loadFromStream(dis),
            XCacheHelper.loadIfNull(dis) ? null : XLoader.loadReadVarSpecs(dis),
            dis.readBoolean()
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private VarAssign(final VarAssign refVarAssign)
    {
        super( refVarAssign.getPath(), refVarAssign.getLNum(), refVarAssign.getCNum() );

        _varName         =                                                      refVarAssign._varName;
        _local           =                                                      refVarAssign._local;
        _const           =                                                      refVarAssign._const;
        _asnSpec         =                                                      refVarAssign._asnSpec;
        _funcCall        = (refVarAssign._funcCall == null) ? null : (FuncCall) refVarAssign._funcCall.deepClone();
        _rvarSpecs       =                                                      refVarAssign._rvarSpecs;
        _isPreTargetExec =                                                      refVarAssign._isPreTargetExec;
    }

    @Override
    public ExecBlock deepClone()
    {
        // This class is not thread-safe; therefore, return a clone of this instance
        return new VarAssign(this);
    }

} // class VarAssign
