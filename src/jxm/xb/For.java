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


public class For extends LoopBlock {

    private final String             _itrVarName;
    private final XCom.VariableStore _itrVarStore;

    private final XCom.ReadVarSpec   _rvarSpecB;
    private final XCom.ReadVarSpec   _rvarSpecE;
    private final XCom.ReadVarSpec   _rvarSpecS;

    private       long               _begVal;
    private       long               _endVal;
    private       long               _stpVal;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private For(
        final String           path,
        final int              lNum,
        final int              cNum,
        final String           itrVarName,
        final XCom.ReadVarSpec rvarSpecB,
        final XCom.ReadVarSpec rvarSpecE,
        final XCom.ReadVarSpec rvarSpecS,
        final boolean          allocExecBlocksElem
    ) {
        super(path, lNum, cNum, allocExecBlocksElem);

        _itrVarName  = XCom.genRVarName(itrVarName);
        _itrVarStore = new XCom.VariableStore(true, null);

        _rvarSpecB   = rvarSpecB;
        _rvarSpecE   = rvarSpecE;
        _rvarSpecS   = rvarSpecS;
    }

    public For(
        final String           path,
        final int              lNum,
        final int              cNum,
        final String           itrVarName,
        final XCom.ReadVarSpec rvarSpecB,
        final XCom.ReadVarSpec rvarSpecE,
        final XCom.ReadVarSpec rvarSpecS
    ) {
        this(path, lNum, cNum, itrVarName, rvarSpecB, rvarSpecE, rvarSpecS, true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean initLoopCondition(final XCom.ExecData execData) throws JXMException
    {
        // Evaluate the begin, end, and step values
        _begVal = XCom.toLong( this, execData, XCom.flatten( execData.execState.readVar(this, execData, _rvarSpecB, true), "" ) );
        _endVal = XCom.toLong( this, execData, XCom.flatten( execData.execState.readVar(this, execData, _rvarSpecE, true), "" ) );
        _stpVal = XCom.toLong( this, execData, XCom.flatten( execData.execState.readVar(this, execData, _rvarSpecS, true), "" ) );

        // Store the iterator value
        _itrVarStore.value = String.valueOf(_begVal);

        execData.execState.setVar(_itrVarName, _itrVarStore, false, false);

        // Done
        return true;
    }

    @Override
    public boolean evalLoopCondition(final XCom.ExecData execData) throws JXMException
    {
        // Get the iterator value
        final long itrVal = Long.parseLong( execData.execState.getVar(this, execData, _itrVarName, true).get(0).value );

        // Evaluate the condition
        return (_stpVal >= 0) ? (itrVal <= _endVal) : (itrVal >= _endVal);
    }

    @Override
    public boolean updtLoopCondition(final XCom.ExecData execData) throws JXMException
    {
        // Get the iterator value
        final long itrVal = Long.parseLong( execData.execState.getVar(this, execData, _itrVarName, true).get(0).value );

        // Store the updated iterator value
        _itrVarStore.value = String.valueOf(itrVal + _stpVal);

        execData.execState.setVar(_itrVarName, _itrVarStore, false, false);

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveString     ( dos, XCom.trmRVarName(_itrVarName) );
        XSaver.saveReadVarSpec( dos, _rvarSpecB                    );
        XSaver.saveReadVarSpec( dos, _rvarSpecE                    );
        XSaver.saveReadVarSpec( dos, _rvarSpecS                    );
    }

    public static For loadFromStream(final DataInputStream dis) throws Exception
    {
        final LoopBlock.LoadLoopData loadLD = loadLDFromStream(dis);

        final For _for = new For(
            loadLD.loadPLC.path,
            loadLD.loadPLC.lNum,
            loadLD.loadPLC.cNum,
            XLoader.loadString(dis),
            XLoader.loadReadVarSpec(dis),
            XLoader.loadReadVarSpec(dis),
            XLoader.loadReadVarSpec(dis),
            false
        );

        _for.setExecBlocks( 0, loadLD.arrayExecBlocks[0] );

        return _for;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private For(final For refFor)
    {
        super( refFor.getPath(), refFor.getLNum(), refFor.getCNum(), false );

        _itrVarName  = refFor._itrVarName;
        _itrVarStore = refFor._itrVarStore.deepClone();

        _rvarSpecB   = refFor._rvarSpecB;
        _rvarSpecE   = refFor._rvarSpecE;
        _rvarSpecS   = refFor._rvarSpecS;

        setExecBlocks( 0, refFor.getExecBlocks(0).deepClone() );
    }

    @Override
    public ExecBlock deepClone()
    {
        // This class is not thread-safe; therefore, return a clone of this instance
        return new For(this);
    }

} // class For
