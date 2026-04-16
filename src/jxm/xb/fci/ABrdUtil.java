/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.util.ArrayList;

import jxm.*;
import jxm.tool.*;
import jxm.xb.*;


public class ABrdUtil {

    public static void _execute_ab_from_file(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the file path
        final String path = SysUtil.resolveAbsolutePath( XCom.flatten( evalVals.get(0), "" ) );

        // Parse the file and convert the result to the encoded representation data
        String edata = null;

        try {
            edata = ArduinoBoardsTxt.parseFromFile(path).toEData();
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }

        // Store the encoded representation data
        retVal.add( new XCom.VariableStore(true, edata) );
    }

    public static void _execute_ab_selector(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the encoded representation data and title
              String edata = XCom.flatten( evalVals.get(0), "" );
        final String title = FuncCall._readFlattenOptParam(evalVals, 1, null);

        // Get the GUI
        try {
            edata = ArduinoBoardsTxt.boardConfigurationGUI(title, edata);
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }

        // Store back the encoded representation data as needed
        if(edata != null) retVal.add( new XCom.VariableStore(true, edata) );
    }

    public static void _execute_ab_getselconf(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the encoded representation data
        final String edata = XCom.flatten( evalVals.get(0), "" );

        // Get the selected map handle for the selected board configuration data
        String handle = null;

        try {
            handle = ArduinoBoardsTxt.getSelectedBoardConfiguration(edata);
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }

        // Store the map handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

} // class ABrdUtil
