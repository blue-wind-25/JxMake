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


public class Repeat extends LoopBlock {

    private final boolean          _forDoWhilst;

    private       XCom.ReadVarSpec _rvarSpec1;
    private       XCom.CompareType _cmpType;
    private       XCom.ReadVarSpec _rvarSpec2;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Repeat(final String path, final int lNum, final int cNum, final boolean forDoWhilst, final boolean allocExecBlocksElem)
    {
        super(path, lNum, cNum, allocExecBlocksElem);

        _forDoWhilst = forDoWhilst;
    }

    public Repeat(final String path, final int lNum, final int cNum, final boolean forDoWhilst)
    { this(path, lNum, cNum, forDoWhilst, true);}

    public void setCondition(
        final XCom.ReadVarSpec rvarSpec1,
        final XCom.CompareType cmpType,
        final XCom.ReadVarSpec rvarSpec2
    ) {
        _rvarSpec1 = rvarSpec1;
        _cmpType   = cmpType;
        _rvarSpec2 = rvarSpec2;
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
    {
        // This function does nothing!
        return true;
    }

    @Override
    public boolean updtLoopCondition(final XCom.ExecData execData) throws JXMException
    {
        final boolean res = evalCmpCondition(execData, _rvarSpec1, _cmpType, _rvarSpec2);

        return _forDoWhilst ? res : !res;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        dos.writeBoolean(_forDoWhilst);

        XSaver.saveReadVarSpec(dos, _rvarSpec1);
        XSaver.saveCompareType(dos, _cmpType  );
        XSaver.saveReadVarSpec(dos, _rvarSpec2);
    }

    public static Repeat loadFromStream(final DataInputStream dis) throws Exception
    {
        final LoopBlock.LoadLoopData loadLD = loadLDFromStream(dis);

        final Repeat _repeat = new Repeat(
            loadLD.loadPLC.path,
            loadLD.loadPLC.lNum,
            loadLD.loadPLC.cNum,
            dis.readBoolean(),
            false
        );

        _repeat.setCondition(
            XLoader.loadReadVarSpec(dis),
            XLoader.loadCompareType(dis),
            XLoader.loadReadVarSpec(dis)
        );

        _repeat.setExecBlocks( 0, loadLD.arrayExecBlocks[0] );

        return _repeat;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Repeat(final Repeat refRepeat)
    {
        super( refRepeat.getPath(), refRepeat.getLNum(), refRepeat.getCNum(), false );

        _forDoWhilst = refRepeat._forDoWhilst;

        _rvarSpec1 = refRepeat._rvarSpec1;
        _cmpType   = refRepeat._cmpType;
        _rvarSpec2 = refRepeat._rvarSpec2;

        setExecBlocks( 0, refRepeat.getExecBlocks(0).deepClone() );
    }

    @Override
    public ExecBlock deepClone()
    {
        // This class is a subclass of the 'LoopBlock' class which in turn is a subclass of the 'ContainerBlock' class,
        // hence, its member execution blocks may not be thread safe; therefore, return a clone of this instance
        return new Repeat(this);
    }

} // class Repeat
