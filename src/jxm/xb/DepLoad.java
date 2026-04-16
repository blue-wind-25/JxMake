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
import jxm.dl.*;


public class DepLoad extends ExecBlock {

    private final XCom.ReadVarSpecs _rvarSpecsT;
    private final boolean           _skipIfNotExist;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public DepLoad(final String path, final int lNum, final int cNum, final XCom.ReadVarSpecs rvarSpecsT, final boolean skipIfNotExist)
    {
        super(path, lNum, cNum);

        _rvarSpecsT     = rvarSpecsT;
        _skipIfNotExist = skipIfNotExist;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Evaluate the argument(s)
        final ArrayList<String> files = new ArrayList<>();

        for(final XCom.ReadVarSpec item : _rvarSpecsT) {
            for( final XCom.VariableStore varStr : execData.execState.readVar(this, execData, item, true) ) {
                files.add(varStr.value);
            }
        }

        // Load the dependency files
        try {
            // NOTE : By design, this function should only ever be called by the main thread;
            //        therefore, thread synchronization should not be required.
            for(final String item : files) GlobalDepLoad.globalLoadDepFile(item, _skipIfNotExist);
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Set the error message
            setErrorFromString( e.toString() );
            return XCom.ExecuteResult.Error;
        }

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

        XSaver.saveReadVarSpecs(dos, _rvarSpecsT);
        dos.writeBoolean(_skipIfNotExist);
    }

    public static DepLoad loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new DepLoad(
            loadPLC.path,
            loadPLC.lNum,
            loadPLC.cNum,
            XLoader.loadReadVarSpecs(dis),
            dis.readBoolean()
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class DepLoad
