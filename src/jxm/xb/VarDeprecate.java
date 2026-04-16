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


public class VarDeprecate extends ExecBlock {

    private final String _varName;
    private final String _repName;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public VarDeprecate(
        final String path,
        final int    lNum,
        final int    cNum,
        final String varName,
        final String repName
    ) {
        super(path, lNum, cNum);

        _varName = varName;
        _repName = repName;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Deprecate the variable
        execData.execState.deprecateVar(_varName, _repName);

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveString(dos, _varName);
        XSaver.saveString(dos, _repName);
    }

    public static VarDeprecate loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new VarDeprecate(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadString(dis),
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

} // class VarDeprecate
