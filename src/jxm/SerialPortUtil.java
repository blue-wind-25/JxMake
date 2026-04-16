/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.ArrayList;

import com.fazecast.jSerialComm.*;

import jxm.xb.*;


public class SerialPortUtil  {

    public static void initialize()
    {
        System.setProperty("fazecast.jSerialComm.appid", "JxMake");

        final String nativeLibPath = "com/fazecast/jSerialComm/native";
        if( SysUtil.isRunFromClassFile() ) {
            System.setProperty( "jSerialComm.library.path"    , SysUtil.resolvePath( nativeLibPath, SysUtil.getJxMakeClassDir().getParent().toString() ) );
        }
        else {
            System.setProperty( "jSerialComm.jar.library.path", nativeLibPath );
        }

        /*
        SysUtil.stdDbg().println(MagicBaudrate   );
        SysUtil.stdDbg().println(LowestBaudrate  );
        SysUtil.stdDbg().println(ResetPulseTimeMS);
        //*/
    }

    public static ArrayList<String> listSerialPort()
    {
        final ArrayList<String> spl = new ArrayList<>();

        final SerialPort[] ports = SerialPort.getCommPorts();

        for(int i = 0; i < ports.length; ++i) spl.add( ports[i].getSystemPortPath() );

        return spl;
    }

    public static void dumpPortList()
    {
        final SerialPort[] ports = SerialPort.getCommPorts();

        for(int i = 0; i < ports.length; ++i) {
            SysUtil.stdOut().printf(
                "[%d] %s (%s) : %s - %s @ %s\n",
                i,
                ports[i].getSystemPortName(),      // ttyUSB0        COM1
                ports[i].getSystemPortPath(),      // /dev/ttyUSB0   \\.\COM1
                ports[i].getDescriptivePortName(),
                ports[i].getPortDescription(),
                ports[i].getPortLocation()
            );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // The magic baudrate that would make the target runs the bootloader
    public static final int MagicBaudrate     = Integer.parseInt( SysUtil.getEnv("JXMAKE_MAGIC_BAUDRATE"     , "1200") );

    // The lowest possible baudrate that would still be supported on most platforms which is not the magic baudrate
    public static final int LowestBaudrate    = Integer.parseInt( SysUtil.getEnv("JXMAKE_LOWEST_BAUDRATE"    , "2400") );
    public static final int LowestBaudrateAlt = Integer.parseInt( SysUtil.getEnv("JXMAKE_LOWEST_BAUDRATE_ALT",  "600") );

    // The reset pulse width
    public static final int ResetPulseTimeMS  = Integer.parseInt( SysUtil.getEnv("JXMAKE_RESET_PULSE_TIME_MS",  "250") );

    private static SerialPort _getSerialPort(final String targetSerialPortPath)
    {
        final SerialPort sp = SerialPort.getCommPort(targetSerialPortPath);

        sp.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        sp.setNumDataBits​(8                               );
        sp.setParity​     (SerialPort.NO_PARITY            );
        sp.setNumStopBits(SerialPort.ONE_STOP_BIT         );
        sp.setRTS        (                                );
        sp.setDTR        (                                );

        return sp;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
    // Set baudrate only
    public static boolean setBaudrate(final String targetSerialPortPath, final int baudrate) throws JXMRuntimeError
    {
        try {
            final SerialPort sp = _getSerialPort(targetSerialPortPath);
            SysUtil.stdDbg().println("#1#");
            sp.setBaudRate​(LowestBaudrate);
            SysUtil.stdDbg().println("#2#");
            sp.openPort   (        );
            SysUtil.stdDbg().println("#3#");
            sp.setBaudRate​(baudrate);
            SysUtil.stdDbg().println("#4#");
            sp.setRTS     (        );
            SysUtil.stdDbg().println("#5#");
            sp.setDTR     (        );
            SysUtil.stdDbg().println("#6#");
            sp.closePort  (        );
            SysUtil.stdDbg().println("#7#");
        }
        catch(final Exception e) {
            return false;
        }

        return true;
    }
    */

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Try to reset the MCU attached to the serial port by using DTR
    public static void tryResetMCU_usingDTR(final String targetSerialPortPath, final int setBaudrateAfterReset) throws JXMRuntimeError
    {
        try {

            // Get the serial port instance
            final SerialPort sp = _getSerialPort(targetSerialPortPath);

            // Open the serial port using the lowest possible baudrate which is not the magic baudrate
            sp.setBaudRate​(LowestBaudrate);
            sp.openPort   (              );

            // Delay for a while
            SysUtil.sleepMS(ResetPulseTimeMS * 2);

            // Send a reset pulse via DTR with RTS high
                           SysUtil.sleepMS(ResetPulseTimeMS);
            sp.setRTS  ();
            sp.clearDTR(); SysUtil.sleepMS(ResetPulseTimeMS);
            sp.setDTR  ();

            // Send a reset pulse via DTR with RTS low
                           SysUtil.sleepMS(ResetPulseTimeMS);
            sp.clearRTS();
            sp.clearDTR(); SysUtil.sleepMS(ResetPulseTimeMS);
            sp.setDTR  ();

            // Change the baudrate after reset if it is specified
            if(setBaudrateAfterReset > 0) sp.setBaudRate​(setBaudrateAfterReset);

            // Close the serial port
            sp.closePort();

        } // try
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Try to reset the MCU attached to the serial port to bootloader mode by using the magic baudrate
    public static void tryResetMCUToBootloader_usingMagicBaudrate(final String targetSerialPortPath, final int magicBaudrate) throws JXMRuntimeError
    {
        try {

            // Get the serial port instance
            final SerialPort sp = _getSerialPort(targetSerialPortPath);

            /*
            // Open and close the serial port using the lowest possible baudrate which is not the magic baudrate
            sp.setBaudRate​(LowestBaudrate);
            sp.openPort   (              );
            sp.closePort  (              );

            // Open the serial port using the magic baudrate
            sp.setBaudRate​(magicBaudrate);
            sp.openPort   (             );

            // Send a reset pulse via DTR with RTS high
                           SysUtil.sleepMS(ResetPulseTimeMS);
            sp.setRTS  ();
            sp.clearDTR(); SysUtil.sleepMS(ResetPulseTimeMS);
            sp.setDTR  ();

            // Close the serial port
            sp.closePort();
            */

            // Open and close the serial port using the lowest possible baudrate which is not the magic baudrate
            sp.setBaudRate​   (LowestBaudrate          );
            sp.openPort      (                        );
            sp.setNumStopBits(SerialPort.TWO_STOP_BITS);
            sp.setNumStopBits(SerialPort.ONE_STOP_BIT );
            sp.closePort     (                        );

            // Delay for a while
            SysUtil.sleepMS(ResetPulseTimeMS);

            // Open and close the serial port using the magic baudrate
            sp.setBaudRate​   (magicBaudrate           );
            sp.openPort      (                        );
            sp.setNumStopBits(SerialPort.TWO_STOP_BITS);
            sp.setNumStopBits(SerialPort.ONE_STOP_BIT );
            sp.closePort     (                        );

            // Delay for a while
            SysUtil.sleepMS(ResetPulseTimeMS * 2);

        } // try
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void tryResetMCUToBootloader_usingMagicBaudrate(final String targetSerialPortPath) throws JXMRuntimeError
    { tryResetMCUToBootloader_usingMagicBaudrate(targetSerialPortPath, MagicBaudrate); }

    // Try to reset the MCU attached to the serial port to bootloader mode by using the specified special byte sequences
    public static void tryResetMCUToBootloader_usingByteSequence(final String targetSerialPortPath, final byte[] seq) throws JXMRuntimeError
    {
        try {

            // List of baudrates to be used (sorted based on the likelihood that they will be used by firmwares out there)
            // ##### ??? TODO : Make configurable using command line option/environment variable ??? #####
            final int[] baudrates = new int[] { 115200, 57600, 19200, 9600, 50000, 100000, 500000, 1000000, 4800, 2400, 1200, 74880 };
          //final int[] baudrates = new int[] { 1200, 2400, 4800, 9600, 19200, 50000, 57600, 74880, 115200, 500000, 100000, 1000000 };

            // Get the serial port instance
            final SerialPort sp = _getSerialPort(targetSerialPortPath);

            // Send the byte sequence using various baudrates
            for(final int baudrate : baudrates) {
                sp.setBaudRate​(baudrate       );
                sp.openPort   (               );
                sp.writeBytes (seq, seq.length); SysUtil.sleepMS(1000 * seq.length * 10 / baudrate + 1);
                sp.closePort  (               );
            }

            // Delay for a while
            SysUtil.sleepMS(ResetPulseTimeMS * 2);

        } // try
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

} // class SerialPortUtil
