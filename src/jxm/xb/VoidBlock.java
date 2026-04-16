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


public class VoidBlock extends ExecBlock {

    public VoidBlock(final String path, final int lNum, final int cNum)
    { super(path, lNum, cNum); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static VoidBlock newErrorVoidBlock00(final String jxmSpecFile_absPath, final String errMsg)
    {
        final VoidBlock voidBlock = new VoidBlock(jxmSpecFile_absPath, 0, 0);
        voidBlock.setErrorFromString(errMsg);

        return voidBlock;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // This function does nothing!
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    { super.saveToStream(dos); }

    public static VoidBlock loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new VoidBlock(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class VoidBlock
