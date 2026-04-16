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


public class IfElse extends ContainerBlock {

    private final XCom.ReadVarSpec _rvarSpec1;
    private final XCom.CompareType _cmpType;
    private final XCom.ReadVarSpec _rvarSpec2;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private IfElse(
        final String           path,
        final int              lNum,
        final int              cNum,
        final XCom.ReadVarSpec rvarSpec1,
        final XCom.CompareType cmpType,
        final XCom.ReadVarSpec rvarSpec2,
        final boolean          allocExecBlocksElem
    ) {
        super(path, lNum, cNum, null, allocExecBlocksElem ? 2 : -2);

        _rvarSpec1 = rvarSpec1;
        _cmpType   = cmpType;
        _rvarSpec2 = rvarSpec2;
    }

    public IfElse(
        final String           path,
        final int              lNum,
        final int              cNum,
        final XCom.ReadVarSpec rvarSpec1,
        final XCom.CompareType cmpType,
        final XCom.ReadVarSpec rvarSpec2
    ) {
        this(path, lNum, cNum, rvarSpec1, cmpType, rvarSpec2, true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Evaluate the condition
        final boolean condRes = evalCmpCondition(execData, _rvarSpec1, _cmpType, _rvarSpec2);

        // Execute the body
        final XCom.ExecuteResult xres = executeBody(execData, condRes ? 0 : 1);

        if( isErrorBlockSet() ) setErrorFromBlock( getErrorBlock() );

        // Done
        return xres;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveReadVarSpec(dos, _rvarSpec1);
        XSaver.saveCompareType(dos, _cmpType  );
        XSaver.saveReadVarSpec(dos, _rvarSpec2);
    }

    public static IfElse loadFromStream(final DataInputStream dis) throws Exception
    {
        final ContainerBlock.LoadContainerData loadCD = loadCDFromStream(dis);

        if(loadCD.blockName != null || loadCD.arrayExecBlocks.length != 2) return null;

        final IfElse ifElse = new IfElse(
            loadCD.loadPLC.path,
            loadCD.loadPLC.lNum,
            loadCD.loadPLC.cNum,
            XLoader.loadReadVarSpec(dis),
            XLoader.loadCompareType(dis),
            XLoader.loadReadVarSpec(dis),
            false
        );

        ifElse.setExecBlocks( 0, loadCD.arrayExecBlocks[0] );
        ifElse.setExecBlocks( 1, loadCD.arrayExecBlocks[1] );

        return ifElse;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private IfElse(final IfElse refIfElse)
    {
        super( refIfElse.getPath(), refIfElse.getLNum(), refIfElse.getCNum(), -2 );

        _rvarSpec1 = refIfElse._rvarSpec1;
        _cmpType   = refIfElse._cmpType;
        _rvarSpec2 = refIfElse._rvarSpec2;

        setExecBlocks( 0, refIfElse.getExecBlocks(0).deepClone() );
        setExecBlocks( 1, refIfElse.getExecBlocks(1).deepClone() );
    }

    @Override
    public ExecBlock deepClone()
    {
        // This class is a subclass of the 'ContainerBlock' class, hence, its member execution blocks
        // may not be thread safe; therefore, return a clone of this instance
        return new IfElse(this);
    }

} // class IfElse
