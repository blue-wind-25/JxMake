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


public class GoTo extends ExecBlock {

    private final XCom.ReadVarSpec _rvarSpec;
    private final boolean          _safeMode;

    private       String           _label;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public GoTo(final String path, final int lNum, final int cNum, final XCom.ReadVarSpec rvarSpec, final boolean safeMode)
    {
        super(path, lNum, cNum);

        _rvarSpec = rvarSpec;
        _safeMode = safeMode;
        _label    = null;
    }

    public String getLabel()
    {
        final String label = _label;
        _label = null;
        return label;
    }

    public boolean isSafeMode()
    { return _safeMode; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Evaluate the argument and combine the result(s)
        _label = XCom.flatten( execData.execState.readVar(this, execData, _rvarSpec, true), "" );

        // Done
        return XCom.ExecuteResult.GoTo;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveReadVarSpec(dos, _rvarSpec);

        dos.writeBoolean(_safeMode);
    }

    public static GoTo loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new GoTo(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadReadVarSpec(dis),
            dis.readBoolean()
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class is not thread-safe; therefore, return a clone of this instance
        return new GoTo(
            getPath(),
            getLNum(),
            getCNum(),
            _rvarSpec,
            _safeMode
        );
    }

} // class GoTo
