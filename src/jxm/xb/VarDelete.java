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


public class VarDelete extends ExecBlock {

    private final String _varName;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public VarDelete(
        final String path,
        final int    lNum,
        final int    cNum,
        final String varName
    ) {
        super(path, lNum, cNum);

        _varName = varName;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Delete (unset) the variable
        execData.execState.unsetVar(_varName);

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveString(dos, _varName);
    }

    public static VarDelete loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new VarDelete(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadString(dis)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class VarDelete
