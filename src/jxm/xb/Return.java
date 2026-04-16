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


public class Return extends ExecBlock {

    private final XCom.ReadVarSpecs  _rvarSpecs;
    private final XCom.VariableValue _retVal;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public Return(final String path, final int lNum, final int cNum, final XCom.ReadVarSpecs rvarSpecs)
    {
        super(path, lNum, cNum);

        _rvarSpecs = rvarSpecs;
        _retVal    = new XCom.VariableValue();
    }

    public XCom.VariableValue getReturnValue()
    { return _retVal;}

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Clear the return value first
        _retVal.clear();

        // Evaluate the argument(s) and combine the result(s)
        for(final XCom.ReadVarSpec item : _rvarSpecs) {
            for( final XCom.VariableStore varStr : execData.execState.readVar(this, execData, item, true) ) {
                _retVal.add(varStr);
            }
        }

        // Done
        return XCom.ExecuteResult.FunctionReturn;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveReadVarSpecs(dos, _rvarSpecs);
    }

    public static Return loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new Return(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadReadVarSpecs(dis)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class is not thread-safe; therefore, return a clone of this instance
        return new Return(
            getPath(),
            getLNum(),
            getCNum(),
            _rvarSpecs
        );
    }

} // class Return
