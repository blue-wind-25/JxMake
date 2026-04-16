/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.util.ArrayList;

import jxm.*;
import jxm.xb.*;


public class SerUtil {

    public static void _execute_ls_ser_ports(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final String item : SerialPortUtil.listSerialPort() ) {
            retVal.add( new XCom.VariableStore(true, item) );
        }
    }

    /*
    // <0> or <1>     $set_baudrate      ( <serial_device_path>, [baudrate      ] )                   # Set the baudrate of the specified and then do nothing
    public static void _execute_set_baudrate(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the after-reset baudrate
        final XCom.VariableValue baudrateStr = evalVals.get(1);
        final int[]              baudrateInt = new int [ baudrateStr.size() ];

        for(int i = 0; i < baudrateInt.length; ++i) baudrateInt[i] = XCom.toLong( execBlock, execData, baudrateStr.get(i).value ).intValue();

        // Try to reset the attached MCU
        int i = 0;
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            final String path = item.value.trim();
            if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "set_baudrate", "<serial_device_path>");
            retVal.add( new XCom.VariableStore(
                true,
                SerialPortUtil.setBaudrate(
                    path,
                    (i < baudrateInt.length) ? baudrateInt[i] : baudrateInt[baudrateInt.length - 1]
                ) ? XCom.Str_T : XCom.Str_F
            ) );
            ++i;
        }
    }
    */

    public static void _execute_mcu_reset(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the after-reset baudrate
        final XCom.VariableValue baudrateStr = FuncCall._getOptParam(evalVals, 1);
        final int[]              baudrateInt = new int[ (baudrateStr != null) ? baudrateStr.size() : 0 ];

        for(int i = 0; i < baudrateInt.length; ++i) baudrateInt[i] = XCom.toLong( execBlock, execData, baudrateStr.get(i).value ).intValue();

        // Try to reset the attached MCU
        int i = 0;
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            final String path = item.value.trim();
            if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "mcu_reset", "<serial_device_path>");
            SerialPortUtil.tryResetMCU_usingDTR( path, (i < baudrateInt.length) ? baudrateInt[i] : 0 );
            ++i;
        }
    }

    public static void _execute_mcu_bootload(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the byte sequence
        byte[] byteSeq = null;

        if( evalVals.size() == 2 ) {
            byteSeq = new byte[ evalVals.get(1).size() ];

            int idx = 0;
            for( final XCom.VariableStore item : evalVals.get(1) ) {
                byteSeq[idx++] = XCom.toLong(execBlock, execData, item.value).byteValue();
            }
        }

        // Try to reset the attached MCU to the bootloader mode
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            final String path = item.value.trim();
            if( path.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyPathStr, "mcu_bootload", "<serial_device_path>");
            if(byteSeq == null) SerialPortUtil.tryResetMCUToBootloader_usingMagicBaudrate(path         );
            else                SerialPortUtil.tryResetMCUToBootloader_usingByteSequence (path, byteSeq);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_sercon(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final boolean tcpMode, final boolean spMode) throws JXMException
    {
        final String device_or_host_or_ip = XCom.flatten( evalVals.get(0), "" );
        final String baudrate_or_port     = XCom.flatten( evalVals.get(1), "" );
        final String new_line_mode        = FuncCall._readFlattenOptParam(evalVals, 2, "native");
        final String cmd_hist_file_path   = FuncCall._readFlattenOptParam(evalVals, 3, null    );

        try {
            // Show the serial console/plotter
            final SerialConsole sc     = new SerialConsole(spMode);
            final String[]      params = {
                                             tcpMode ? "tcp" : "tty",
                                             device_or_host_or_ip,
                                             baudrate_or_port,
                                             new_line_mode,
                                             (cmd_hist_file_path != null) ? cmd_hist_file_path
                                                                          : SysUtil.resolvePath( SerialConsole.DEFAULT_CMD_HIST_F_NAME, SysUtil.getADD() )
                                         };
            final String       outText = sc.showConsole(params);
            // Check for error
            if(outText == null) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_SerialCPError, tcpMode ? "tcp_" : "", spMode ? "plotter" : "console");
            // Return the output text
            if(!spMode) retVal.add( new XCom.VariableStore(true, outText) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

} // class SerUtil
