/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.ArrayList;

import jxm.*;


public class ShellCmdDef extends ExecBlock {

    private final XCom.ReadVarSpecs _rvarSpecs;
    private final byte              _soFlag;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ShellCmdDef(final String path, final int lNum, final int cNum, final XCom.ReadVarSpecs rvarSpecs, final byte soFlag)
    {
        super(path, lNum, cNum);

        _rvarSpecs = rvarSpecs;
        _soFlag    = soFlag;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Error if shell operation is not enabled
        if(!execData.enShellOper) throw XCom.newJXMRuntimeError(Texts.EMsg_UserFuncNoShellOper);

        // Evaluate the argument(s)
        final ArrayList<XCom.VariableValue> evalVals = new ArrayList<>();

        for(final XCom.ReadVarSpec item : _rvarSpecs) {
            evalVals.add( execData.execState.readVar(this, execData, item, true) );
        }

        // Add the command(s)
        execData.shellOper.addCommand( evalVals, getPath(), getLNum(), getCNum(), _soFlag );

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveReadVarSpecs(dos, _rvarSpecs);

        dos.writeByte(_soFlag);
    }

    public static ShellCmdDef loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new ShellCmdDef(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadReadVarSpecs(dis),
            dis.readByte()
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class ShellCmdDef
