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


public class ForEach extends LoopBlock {

    private final String             _idxVarName;
    private final XCom.VariableStore _idxVarStore;

    private final String             _valVarName;
    private final XCom.VariableStore _valVarStore;

    private final XCom.ReadVarSpec   _rvarSpecM;
    private final XCom.ReadVarSpec   _rvarSpecK;
    private final XCom.ReadVarSpec   _rvarSpecS;

    private       XCom.VariableValue _setVal;
    private       int                _idxVal;
    private       int                _endVal;
    private       int                _stpVal;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ForEach(
        final String           path,
        final int              lNum,
        final int              cNum,
        final String           idxVarName,
        final String           valVarName,
        final XCom.ReadVarSpec rvarSpecM,
        final XCom.ReadVarSpec rvarSpecK,
        final XCom.ReadVarSpec rvarSpecS,
        final boolean          allocExecBlocksElem
    ) {
        super(path, lNum, cNum, allocExecBlocksElem);

        _idxVarName  = XCom.genRVarName(idxVarName);
        _idxVarStore = new XCom.VariableStore(true, null);

        _valVarName  = XCom.genRVarName(valVarName);
        _valVarStore = new XCom.VariableStore(true, null);

        _rvarSpecM   = rvarSpecM;
        _rvarSpecK   = rvarSpecK;
        _rvarSpecS   = rvarSpecS;
    }

    public ForEach(
        final String           path,
        final int              lNum,
        final int              cNum,
        final String           idxVarName,
        final String           valVarName,
        final XCom.ReadVarSpec rvarSpecM,
        final XCom.ReadVarSpec rvarSpecK,
        final XCom.ReadVarSpec rvarSpecS
    ) {
        this(path, lNum, cNum, idxVarName, valVarName, rvarSpecM, rvarSpecK, rvarSpecS, true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean initLoopCondition(final XCom.ExecData execData) throws JXMException
    {
        // Evaluate the set, index, end, and step values
        _setVal =                                            execData.execState.readVar(this, execData, _rvarSpecM, true)                   ;
        _idxVal = XCom.toLong( this, execData, XCom.flatten( execData.execState.readVar(this, execData, _rvarSpecK, true), "" ) ).intValue();
        _endVal =                                                                                       _setVal.size()                      ;
        _stpVal = XCom.toLong( this, execData, XCom.flatten( execData.execState.readVar(this, execData, _rvarSpecS, true), "" ) ).intValue();

        // Check the index value
        if(_idxVal >= _endVal) return false;

        // Store the index value as needed
        if(_idxVarName != null) {
            _idxVarStore.value = String.valueOf(_idxVal + 1);
            execData.execState.setVar(_idxVarName, _idxVarStore, false, false);
        }

        // Store the set member value
        _valVarStore.value = _setVal.get(_idxVal).value;
        execData.execState.setVar(_valVarName, _valVarStore, false, false);

        // Done
        return true;
    }

    @Override
    public boolean evalLoopCondition(final XCom.ExecData execData) throws JXMException
    {
        // This function does nothing!
        return true;
    }

    @Override
    public boolean updtLoopCondition(final XCom.ExecData execData) throws JXMException
    {
        // Update and check the index value
        _idxVal += _stpVal;

        if(_idxVal >= _endVal) return false;

        // Store the index value as needed
        if(_idxVarName != null) {
            _idxVarStore.value = String.valueOf(_idxVal + 1);
            execData.execState.setVar(_idxVarName, _idxVarStore, false, false);
        }

        // Update and store the set member value
        _valVarStore.value = _setVal.get(_idxVal).value;
        execData.execState.setVar(_valVarName, _valVarStore, false, false);

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveString     ( dos, XCom.trmRVarName(_idxVarName) );
        XSaver.saveString     ( dos, XCom.trmRVarName(_valVarName) );

        XSaver.saveReadVarSpec( dos, _rvarSpecM                    );
        XSaver.saveReadVarSpec( dos, _rvarSpecK                    );
        XSaver.saveReadVarSpec( dos, _rvarSpecS                    );
    }

    public static ForEach loadFromStream(final DataInputStream dis) throws Exception
    {
        final LoopBlock.LoadLoopData loadLD = loadLDFromStream(dis);

        final ForEach _foreach = new ForEach(
            loadLD.loadPLC.path,
            loadLD.loadPLC.lNum,
            loadLD.loadPLC.cNum,
            XLoader.loadString(dis),
            XLoader.loadString(dis),
            XLoader.loadReadVarSpec(dis),
            XLoader.loadReadVarSpec(dis),
            XLoader.loadReadVarSpec(dis),
            false
        );

        _foreach.setExecBlocks( 0, loadLD.arrayExecBlocks[0] );

        return _foreach;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ForEach(final ForEach refForEach)
    {
        super( refForEach.getPath(), refForEach.getLNum(), refForEach.getCNum(), false );

        _idxVarName  = refForEach._idxVarName;
        _idxVarStore = refForEach._idxVarStore.deepClone();

        _valVarName  = refForEach._valVarName;
        _valVarStore = refForEach._valVarStore.deepClone();

        _rvarSpecM   = refForEach._rvarSpecM;
        _rvarSpecK   = refForEach._rvarSpecK;
        _rvarSpecS   = refForEach._rvarSpecS;

        setExecBlocks( 0, refForEach.getExecBlocks(0).deepClone() );
    }

    @Override
    public ExecBlock deepClone()
    {
        // This class is not thread-safe; therefore, return a clone of this instance
        return new ForEach(this);
    }

} // class ForEach
