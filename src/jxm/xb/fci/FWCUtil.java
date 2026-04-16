/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.util.ArrayList;

import jxm.*;
import jxm.xb.*;


public class FWCUtil {

    @FunctionalInterface private static interface M_Void_Void { void apply() throws Exception; }

    private static void __xCall_translateException(final M_Void_Void lambda) throws JXMException
    {
        try {
            // Call the lambda function
            lambda.apply();
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_fwc_new(final XCom.VariableValue retVal) throws JXMException
    {
        // Create a new FWC and store its handle
        retVal.add( new XCom.VariableStore( true, FWCList.fwcNew() ) );
    }

    public static void _execute_fwc_delete(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Delete the FWC
        __xCall_translateException( () -> { FWCList.fwcDelete(handle); } );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_fwc_ld_rbin(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the path and start-address-offset
        final String path = XCom.flatten( evalVals.get(0), "" );
        final long   sao  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") );

        // Create a new FWC
        final String handle = FWCList.fwcNew();

        // Load the file
        __xCall_translateException( () -> { FWCList.fwcLoadRawBinaryFile( handle, SysUtil.resolveAbsolutePath(path), sao ); } );

        // Store the FWC handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

    public static void _execute_fwc_ld_elfb(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the path and start-address-offset
        final String path = XCom.flatten( evalVals.get(0), "" );
        final long   sao  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") );

        // Get the 'ELF_ISecRules' and 'ELF_ESecRules' JSON strings
        final String isrJSONStr = FuncCall._readFlattenOptParam(evalVals, 2, ""       ).trim();
        final String esrJSONStr = FuncCall._readFlattenOptParam(evalVals, 3, "DEFAULT").trim();

        // Create a new FWC
        final String handle = FWCList.fwcNew();

        // Load the file
        __xCall_translateException( () -> { FWCList.fwcLoadELFBinaryFile( handle, SysUtil.resolveAbsolutePath(path), sao, isrJSONStr, esrJSONStr ); } );

        // Store the FWC handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

    public static void _execute_fwc_ld_ihex(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the path and start-address-offset
        final String path = XCom.flatten( evalVals.get(0), "" );
        final long   sao  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") );

        // Create a new FWC
        final String handle = FWCList.fwcNew();

        // Load the file
        __xCall_translateException( () -> { FWCList.fwcLoadIntelHexFile( handle, SysUtil.resolveAbsolutePath(path), sao ); } );

        // Store the FWC handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

    public static void _execute_fwc_ld_msrec(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the path and start-address-offset
        final String path = XCom.flatten( evalVals.get(0), "" );
        final long   sao  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") );

        // Create a new FWC
        final String handle = FWCList.fwcNew();

        // Load the file
        __xCall_translateException( () -> { FWCList.fwcLoadMotorolaSRecordFile( handle, SysUtil.resolveAbsolutePath(path), sao ); } );

        // Store the FWC handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

    public static void _execute_fwc_ld_thex(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the path and start-address-offset
        final String path = XCom.flatten( evalVals.get(0), "" );
        final long   sao  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") );

        // Create a new FWC
        final String handle = FWCList.fwcNew();

        // Load the file
        __xCall_translateException( () -> { FWCList.fwcLoadTektronixHexFile( handle, SysUtil.resolveAbsolutePath(path), sao ); } );

        // Store the FWC handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

    public static void _execute_fwc_ld_mostc(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the path and start-address-offset
        final String path = XCom.flatten( evalVals.get(0), "" );
        final long   sao  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") );

        // Create a new FWC
        final String handle = FWCList.fwcNew();

        // Load the file
        __xCall_translateException( () -> { FWCList.fwcLoadMOSTechnologyFile( handle, SysUtil.resolveAbsolutePath(path), sao ); } );

        // Store the FWC handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

    public static void _execute_fwc_ld_tixhex(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the path and start-address-offset
        final String path = XCom.flatten( evalVals.get(0), "" );
        final long   sao  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") );

        // Create a new FWC
        final String handle = FWCList.fwcNew();

        // Load the file
        __xCall_translateException( () -> { FWCList.fwcLoadTITextHexFile( handle, SysUtil.resolveAbsolutePath(path), sao ); } );

        // Store the FWC handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

    public static void _execute_fwc_ld_aschex(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the path and start-address-offset
        final String path = XCom.flatten( evalVals.get(0), "" );
        final long   sao  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") );

        // Create a new FWC
        final String handle = FWCList.fwcNew();

        // Load the file
        __xCall_translateException( () -> { FWCList.fwcLoadASCIIHexFile( handle, SysUtil.resolveAbsolutePath(path), sao ); } );

        // Store the FWC handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

    public static void _execute_fwc_ld_vlvmem(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the path and start-address-offset
        final String path = XCom.flatten( evalVals.get(0), "" );
        final long   sao  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "0") );

        // Create a new FWC
        final String handle = FWCList.fwcNew();

        // Load the file
        __xCall_translateException( () -> { FWCList.fwcLoadVerilogVMemFile( handle, SysUtil.resolveAbsolutePath(path), sao ); } );

        // Store the FWC handle
        retVal.add( new XCom.VariableStore(true, handle) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_fwc_sv_rbin(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the handle, path, and null-byte
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String path   = XCom.flatten( evalVals.get(1), "" );
        final byte   nbyte  = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "0") ).byteValue();

        // Save the file
        __xCall_translateException( () -> { FWCList.fwcSaveRawBinaryFile( handle, SysUtil.resolveAbsolutePath(path), nbyte ); } );
    }

    public static void _execute_fwc_sv_ihex(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the handle and path
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String path   = XCom.flatten( evalVals.get(1), "" );

        // Save the file
        __xCall_translateException( () -> { FWCList.fwcSaveIntelHexFile( handle, SysUtil.resolveAbsolutePath(path), (byte) 0 ); } );
    }

    public static void _execute_fwc_sv_msrec(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the handle and path
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String path   = XCom.flatten( evalVals.get(1), "" );

        // Save the file
        __xCall_translateException( () -> { FWCList.fwcSaveMotorolaSRecordFile( handle, SysUtil.resolveAbsolutePath(path), (byte) 0 ); } );
    }

    public static void _execute_fwc_sv_thex(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the handle and path
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String path   = XCom.flatten( evalVals.get(1), "" );

        // Save the file
        __xCall_translateException( () -> { FWCList.fwcSaveTektronixHexFile( handle, SysUtil.resolveAbsolutePath(path), (byte) 0 ); } );
    }

    public static void _execute_fwc_sv_mostc(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the handle and path
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String path   = XCom.flatten( evalVals.get(1), "" );

        // Save the file
        __xCall_translateException( () -> { FWCList.fwcSaveMOSTechnologyFile( handle, SysUtil.resolveAbsolutePath(path), (byte) 0 ); } );
    }

    public static void _execute_fwc_sv_tixhex(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the handle and path
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String path   = XCom.flatten( evalVals.get(1), "" );

        // Save the file
        __xCall_translateException( () -> { FWCList.fwcSaveTITextHexFile( handle, SysUtil.resolveAbsolutePath(path), (byte) 0 ); } );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_fwc_clear(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Clear the FWC
        __xCall_translateException( () -> { FWCList.fwcClear(handle); } );
    }

    public static void _execute_fwc_min_saddr(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Get and store the address
        __xCall_translateException( () -> { retVal.add( new XCom.VariableStore( true, String.valueOf( FWCList.fwcMinStartAddress(handle) ) ) ); } );
    }

    public static void _execute_fwc_max_faddr(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Get and store the address
        __xCall_translateException( () -> { retVal.add( new XCom.VariableStore( true, String.valueOf( FWCList.fwcMaxFinalAddress(handle) ) ) ); } );
    }

    public static void _execute_fwc_equals(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the handles
        final String  handle1   = XCom.flatten  ( evalVals.get(0), "" );
        final String  handle2   = XCom.flatten  ( evalVals.get(1), "" );
        final boolean flattened = XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "false") );

        // Compare and store the result
        __xCall_translateException( () -> { retVal.add( new XCom.VariableStore( true, String.valueOf( FWCList.fwcEquals(handle1, handle2, flattened) ) ) ); } );
    }

    public static void _execute_fwc_compose(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the handles and address-offset
        final String handleDst = XCom.flatten( evalVals.get(0), "" );
        final String handleSrc = XCom.flatten( evalVals.get(1), "" );
        final long   aofsSrc   = XCom.toLong ( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "0") );

        // Compose and store the status result
        __xCall_translateException( () -> { retVal.add( new XCom.VariableStore( true, String.valueOf( FWCList.fwcCompose(handleDst, handleSrc, aofsSrc) ) ) ); } );
    }

} // class FWCUtil
