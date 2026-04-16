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


public class While extends LoopBlock {

    private final XCom.ReadVarSpec _rvarSpec1;
    private final XCom.CompareType _cmpType;
    private final XCom.ReadVarSpec _rvarSpec2;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private While(
        final String           path,
        final int              lNum,
        final int              cNum,
        final XCom.ReadVarSpec rvarSpec1,
        final XCom.CompareType cmpType,
        final XCom.ReadVarSpec rvarSpec2,
        final boolean          allocExecBlocksElem
    ) {
        super(path, lNum, cNum, allocExecBlocksElem);

        _rvarSpec1 = rvarSpec1;
        _cmpType   = cmpType;
        _rvarSpec2 = rvarSpec2;
    }

    public While(
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
    public boolean initLoopCondition(final XCom.ExecData execData) throws JXMException
    {
        // This function does nothing!
        return true;
    }

    @Override
    public boolean evalLoopCondition(final XCom.ExecData execData) throws JXMException
    { return evalCmpCondition(execData, _rvarSpec1, _cmpType, _rvarSpec2); }

    @Override
    public boolean updtLoopCondition(final XCom.ExecData execData) throws JXMException
    {
        // This function does nothing!
        return true;
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

    public static While loadFromStream(final DataInputStream dis) throws Exception
    {
        final LoopBlock.LoadLoopData loadLD = loadLDFromStream(dis);

        final While _while = new While(
            loadLD.loadPLC.path,
            loadLD.loadPLC.lNum,
            loadLD.loadPLC.cNum,
            XLoader.loadReadVarSpec(dis),
            XLoader.loadCompareType(dis),
            XLoader.loadReadVarSpec(dis),
            false
        );

        _while.setExecBlocks( 0, loadLD.arrayExecBlocks[0] );

        return _while;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private While(final While refWhile)
    {
        super( refWhile.getPath(), refWhile.getLNum(), refWhile.getCNum(), false );

        _rvarSpec1 = refWhile._rvarSpec1;
        _cmpType   = refWhile._cmpType;
        _rvarSpec2 = refWhile._rvarSpec2;

        setExecBlocks( 0, refWhile.getExecBlocks(0).deepClone() );
    }

    @Override
    public ExecBlock deepClone()
    {
        // This class is a subclass of the 'LoopBlock' class which in turn is a subclass of the 'ContainerBlock' class,
        // hence, its member execution blocks may not be thread safe; therefore, return a clone of this instance
        return new While(this);
    }

} // class While
