/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.lang.Math;

import jxm.*;


public class ALOperation extends ExecBlock {

    private final String           _resVarName;
    private final XCom.ALOperName  _operName;

    private final XCom.ReadVarSpec _rvarSpecR;
    private final XCom.ReadVarSpec _rvarSpec1;
    private final XCom.ReadVarSpec _rvarSpec2;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ALOperation(
        final String           path,
        final int              lNum,
        final int              cNum,
        final String           resVarName,
        final XCom.ALOperName  operName,
        final XCom.ReadVarSpec rvarSpecR,
        final XCom.ReadVarSpec rvarSpec1,
        final XCom.ReadVarSpec rvarSpec2
    ) {
        super(path, lNum, cNum);

        _resVarName = XCom.genRVarName(resVarName);
        _operName   = operName;
        _rvarSpecR  = rvarSpecR;
        _rvarSpec1  = rvarSpec1;
        _rvarSpec2  = rvarSpec2;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public XCom.ExecuteResult execute(final XCom.ExecData execData) throws JXMException
    {
        // Evaluate the argument(s)
        final String evalStr1 = XCom.flatten( execData.execState.readVar(this, execData, _rvarSpec1, true), "" );
        final String evalStr2 = XCom.flatten( execData.execState.readVar(this, execData, _rvarSpec2, true), "" );

        // Get the integers(s)
        final long value1 = evalStr1.isEmpty() ? 0 : XCom.toLong(this, execData, evalStr1);
        final long value2 = evalStr2.isEmpty() ? 0 : XCom.toLong(this, execData, evalStr2);

        // Perform the calculation
        long result = 0;

        if(_rvarSpecR != null) {
            // If this is an operation without arguments, evaluate the result variable to integer
            final String evalStrR = XCom.flatten( execData.execState.readVar(this, execData, _rvarSpecR, true), "" );
            result = XCom.toLong(this, execData, evalStrR);
        }

        switch(_operName) {

            case inc : result =           result +  1      ; break;
            case dec : result =           result -  1      ; break;

            case add : result =           value1 +  value2 ; break;
            case sub : result =           value1 -  value2 ; break;
            case mul : result =           value1 *  value2 ; break;
            case div : result =           value1 /  value2 ; break;
            case mod : result =           value1 %  value2 ; break;

            case abs : result =  Math.abs(value1          ); break;
            case neg : result =          -value1           ; break;

            case shl : result =           value1 << value2 ; break;
            case shr : result =           value1 >> value2 ; break;

            case min : result =  Math.min(value1,   value2); break;
            case max : result =  Math.max(value1,   value2); break;

            case not : result =          ~value1           ; break;
            case and : result =           value1 &  value2 ; break;
            case or  : result =           value1 |  value2 ; break;
            case xor : result =           value1 ^  value2 ; break;

        } // switch

        // Store the result
        execData.execState.setVar( _resVarName, new XCom.VariableStore( true, String.valueOf(result) ), false, false );

        // Done
        return XCom.ExecuteResult.Done;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void saveToStream(final DataOutputStream dos) throws IOException
    {
        super.saveToStream(dos);

                                                        XSaver.saveString     ( dos, XCom.trmRVarName(_resVarName) );
                                                        XSaver.saveALOperName ( dos, _operName                     );
        if( !XCacheHelper.saveIfNull(dos, _rvarSpecR) ) XSaver.saveReadVarSpec( dos, _rvarSpecR                    );
                                                        XSaver.saveReadVarSpec( dos, _rvarSpec1                    );
                                                        XSaver.saveReadVarSpec( dos, _rvarSpec2                    );
    }

    public static ALOperation loadFromStream(final DataInputStream dis) throws Exception
    {
        final ExecBlock.LoadPLC loadPLC = ExecBlock.loadPLCFromStream(dis);

        return new ALOperation(
                                                  loadPLC.path,
                                                  loadPLC.lNum,
                                                  loadPLC.cNum,
                                                  XLoader.loadString     (dis),
                                                  XLoader.loadALOperName (dis),
            XCacheHelper.loadIfNull(dis) ? null : XLoader.loadReadVarSpec(dis),
                                                  XLoader.loadReadVarSpec(dis),
                                                  XLoader.loadReadVarSpec(dis)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ExecBlock deepClone()
    {
        // This class should be thread-safe; therefore, simply return a reference to this instance
        return this;
    }

} // class ALOperation
