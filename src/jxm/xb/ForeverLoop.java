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


public class ForeverLoop extends LoopBlock {

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ForeverLoop(final String path, final int lNum, final int cNum, final boolean allocExecBlocksElem)
    { super(path, lNum, cNum, false); }

    public ForeverLoop(final String path, final int lNum, final int cNum)
    { super(path, lNum, cNum, true); }

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
        // This function does nothing!
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    { super.saveToStream(dos); }

    public static ForeverLoop loadFromStream(final DataInputStream dis) throws Exception
    {
        final LoopBlock.LoadLoopData loadLD = loadLDFromStream(dis);

        final ForeverLoop _foreverloop = new ForeverLoop(
            loadLD.loadPLC.path,
            loadLD.loadPLC.lNum,
            loadLD.loadPLC.cNum,
            false
        );

        _foreverloop.setExecBlocks( 0, loadLD.arrayExecBlocks[0] );

        return _foreverloop;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class ForeverLoop
