/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.util.ArrayList;

import jxm.*;
import jxm.xb.*;


public class XMLFUtil {

    public static void _execute_xmlframe(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final boolean fromString) throws JXMException
    {
        try {

            // Get the path or string
            final String arg = XCom.flatten( evalVals.get(0), fromString ? "\n" : "" ).trim();

            if( arg.isEmpty() ) {

                throw fromString ? XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyXMLFrameStr, "string", "<xml_string>"   )
                                 : XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyXMLFrameStr, "file"  , "<xml_file_path>");
            }

            // Execute
            final ArrayList<XMLFrame.Result> res = fromString ? XMLFrame.executeFromXMLString(arg)
                                                              : XMLFrame.executeFromXMLFile  (arg);

            if( res == null || res.isEmpty() ) return;

            // Return the result
            for(final XMLFrame.Result r : res) {
                retVal.add( new XCom.VariableStore(true, r.type ) );
                retVal.add( new XCom.VariableStore(true, r.value) );
            }

        } // try
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_xml_escape(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            retVal.add( new XCom.VariableStore( true, XMLFrame.escapeEntity(item.value) ) );
        }
    }

} // class XMLFUtil
