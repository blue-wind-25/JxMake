/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.Arrays;
import java.util.ArrayList;

import com.fazecast.jSerialComm.*;

import jxm.*;
import jxm.xb.*;


/*
 * Please refer to '../../../docs/txt/en_US/14-Simple-Programmer-Hardware.txt' (and its translations) for the connection diagram of some simple programmers.
 */
public class USB_GPIO extends USB2GPIO {

    private static final String DevClassName       = "USB_GPIO:";

    private static final String DevClassNameSystem = DevClassName + DevSubClassNameSystem;

    private static final String DevClassNameBBSPI  = null;                                          // Not supported!
    private static final String DevClassNameBBUSRT = DevClassName + USB2GPIO.DevSubClassNameBBUSRT; // Master mode only -        half duplex
    private static final String DevClassNameBBUART = null;                                          // Not supported!

    private static final String DevClassNameHWSPI  = DevClassName + USB2GPIO.DevSubClassNameHWSPI;  // Master mode only - full        duplex
    private static final String DevClassNameHWUSRT = DevClassName + USB2GPIO.DevSubClassNameHWUSRT; // Master mode only - full & half duplex
    private static final String DevClassNameHWUART = DevClassName + USB2GPIO.DevSubClassNameHWUART; // Master mode only - full & half duplex

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int[] PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE = new int[] { 255, 512 };

    private static final int   REOPEN_PORT_TIMEOUT_MS              = 5000;
    private static final int   TX_RX_TIMEOUT_MS                    = 1000;

    private static final int   CMD_PREFIX                          = 0xAA; // All commands must be prefixed with this character

    private static final int   CMD_RES_ACK                         = 0xF0; // Command status result
    private static final int   CMD_RES_NAK                         = 0xFF;

    private static final int   CMD_INVALID                         = 0xFF; // Invalid command

    private static final int   CMD_PING                            = 0x00; // System   commands - starts from 0x00
    private static final int   CMD_GET_PROTOCOL_VERSION            = 0x01;
    private static final int   CMD_GET_FIRMWARE_VERSION            = 0x02;
    private static final int   CMD_ENABLE_DEBUG_MESSAGE            = 0x05;
    private static final int   CMD_RESET                           = 0x08;
    private static final int   CMD_RESET_TO_BOOTLOADER             = 0x09;
    private static final int   CMD_DETECT                          = 0x0D;

    private static final int   CMD_HW_GPIO_SET_MODE                = 0x20; // HW-GPIO  commands - starts from 0x20
    public  static final int       HW_GPIO_SET_MODE_INP            = 0x00;
    public  static final int       HW_GPIO_SET_MODE_INP_PU         = 0x01;
    public  static final int       HW_GPIO_SET_MODE_OUT            = 0x02;
    public  static final int       HW_GPIO_SET_MODE_NO_CHG         = 0x03;
    private static final int   CMD_HW_GPIO_SET_VALUES              = 0x21;
    private static final int   CMD_HW_GPIO_GET_VALUES              = 0x22;
    private static final int   CMD_HW_GPIO_SET_PWM                 = 0x23;
    private static final int   CMD_HW_GPIO_GET_ADC                 = 0x24;
    private static final int   CMD_HW_GPIO_GET_VREAD               = 0x25;
    private static final int   CMD_HW_GPIO_CALIBRATE_VREAD         = 0x26;

    private static final int   CMD_HW_SPI_ENABLE                   = 0x40; // HW-SPI   commands - starts from 0x40
    private static final int   CMD_HW_SPI_DISABLE                  = 0x41;
    private static final int   CMD_HW_SPI_SELECT_SLAVE             = 0x42;
    private static final int   CMD_HW_SPI_DESELECT_SLAVE           = 0x43;
    private static final int   CMD_HW_SPI_TRANSFER                 = 0x44;
    private static final int   CMD_HW_SPI_SET_SCK_FREQUENCY        = 0x45;
    private static final int   CMD_HW_SPI_SET_SPI_MODE             = 0x46;
    private static final int   CMD_HW_SPI_SET_CLR_BREAK            = 0x48;
    private static final int   CMD_HW_SPI_SET_CLR_BREAK_EXT        = 0x49;
    private static final int   CMD_HW_SPI_XB_TRANSFER              = 0x4A;
    private static final int   CMD_HW_SPI_XB_SPECIAL               = 0x4C;
    private static final int   CMD_HW_SPI_TRANSFER_W16ND_R16DN     = 0x50;
    private static final int       HW_SPI_XBS_DSPIC30_HV_ESQ       = 1;
    private static final int       HW_SPI_XBS_DSPIC33_LV_ESQ       = 2;
    private static final int[]     HW_SPI_TRANSFER_MAX_SIZE        = PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE;

    private static final int   CMD_HW_TWI_ENABLE                   = 0x60; // HW-TWI   commands - starts from 0x60
    private static final int   CMD_HW_TWI_DISABLE                  = 0x61;
    private static final int   CMD_HW_TWI_WRITE                    = 0x62;
    private static final int   CMD_HW_TWI_WRITE_NO_STOP            = 0x63;
    private static final int   CMD_HW_TWI_READ                     = 0x64;
    private static final int   CMD_HW_TWI_READ_NO_STOP             = 0x65;
    private static final int   CMD_HW_TWI_IS_ENABLED               = 0x69;
    private static final int   CMD_HW_TWI_SCAN                     = 0x6A;
    private static final int   CMD_HW_TWI_WRITE_ONE_CF             = 0x6C;
    private static final int   CMD_HW_TWI_READ_ONE_CF              = 0x6D;
    private static final int[]     HW_TWI_TRANSFER_MAX_SIZE        = PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE;

    private static final int   CMD_HW_UART_ENABLE                  = 0x80; // HW-UART  commands - starts from 0x80
    private static final int   CMD_HW_UART_DISABLE                 = 0x81;
    private static final int   CMD_HW_UART_ENABLE_TX               = 0x84;
    private static final int   CMD_HW_UART_DISABLE_TX              = 0x85;
    private static final int   CMD_HW_UART_DISABLE_TX_AFTER        = 0x86;

    private static final int   CMD_HW_USRT_ENABLE                  = 0xA0; // HW-USRT commands - starts from 0xA0
    private static final int   CMD_HW_USRT_DISABLE                 = 0xA1;
    private static final int   CMD_HW_USRT_SELECT_SLAVE            = 0xA2;
    private static final int   CMD_HW_USRT_DESELECT_SLAVE          = 0xA3;
    private static final int   CMD_HW_USRT_ENABLE_TX               = 0xA4;
    private static final int   CMD_HW_USRT_DISABLE_TX              = 0xA5;
    private static final int   CMD_HW_USRT_DISABLE_TX_AFTER        = 0xA6;

    private static final int   CMD_BB_USRT_ENABLE                  = 0xB0; // BB-USRT commands - starts from 0xB0
    private static final int       BB_USRT_PARITY_NONE             = 0x00;
    private static final int       BB_USRT_PARITY_EVEN             = 0x20;
    private static final int       BB_USRT_PARITY_ODD              = 0x30;
    private static final int       BB_USRT_STOP_BIT_1              = 0x00;
    private static final int       BB_USRT_STOP_BIT_2              = 0x08;
    private static final int       BB_USRT_SS_ACTIVE_LOW           = 0x00;
    private static final int       BB_USRT_SS_ACTIVE_HIGH          = 0x01;
    private static final int   CMD_BB_USRT_DISABLE                 = 0xB1;
    private static final int   CMD_BB_USRT_SELECT_SLAVE            = 0xB2;
    private static final int   CMD_BB_USRT_DESELECT_SLAVE          = 0xB3;
    private static final int   CMD_BB_USRT_PULSE_XCK               = 0xB4;
    private static final int   CMD_BB_USRT_TX                      = 0xB5;
    private static final int   CMD_BB_USRT_RX                      = 0xB6;
    private static final int[]     BB_USRT_TX_RX_MAX_SIZE          = PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE;

    private static final int   CMD_BB_SWIM_ENABLE                  = 0xD0; // BB-SWIM commands - starts from 0xD0
    private static final int   CMD_BB_SWIM_DISABLE                 = 0xD1;
    private static final int   CMD_BB_SWIM_LINE_RESET              = 0xD2;
    private static final int   CMD_BB_SWIM_TRANSFER                = 0xD3;
    private static final int   CMD_BB_SWIM_SRST                    = 0xD4;
    private static final int   CMD_BB_SWIM_ROTF                    = 0xD5;
    private static final int   CMD_BB_SWIM_WOTF                    = 0xD6;
    private static final int[]     BB_SWIM_TRANSFER_MAX_SIZE       = PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE;

    private static final int   CMD_JTAG_ENABLE                     = 0xF0; // JTAG commands - starts from 0xF0
    private static final int   CMD_JTAG_DISABLE                    = 0xF1;
    private static final int   CMD_JTAG_SET_FREQUENCY              = 0xF2;
    private static final int   CMD_JTAG_SET_RESET                  = 0xF3;
    private static final int   CMD_JTAG_TMS                        = 0xF5;
    private static final int   CMD_JTAG_TRANSFER                   = 0xF7;
    private static final int   CMD_JTAG_XB_TRANSFER                = 0xF8;
    private static final int[]     JTAG_TRANSFER_MAX_SIZE          = PROTOCOL_ARGUMENT_STORE_BUFFER_SIZE;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static byte[] _buildCmdBytes(final int numBytes, final int... bytes)
    {
        final byte[] cmd = new byte[numBytes + 1];

                                          cmd[0    ] = (byte) CMD_PREFIX;
        for(int i = 0; i < numBytes; ++i) cmd[i + 1] = (byte) bytes[i];

        return cmd;
    }

    private static boolean _checkAck(final int res)
    { return res == CMD_RES_ACK; }

    private static boolean _checkAck(final int[] res)
    { return res != null && _checkAck(res[0]); }

    private static boolean _checkNak(final int res)
    { return res == CMD_RES_NAK; }

    private static boolean _checkNak(final int[] res)
    { return res != null && _checkNak(res[0]); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _autoNotifyErrorMessage = false;
    private String  _lastErrorMessage       = null;

    private boolean _writeNCmd_impl_part(final byte[] bytes)
    {
        // Prepare the timeout
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS);

        // Write the byte(s)
        int len = bytes.length;
        int ofs = 0;

        while(len != 0) {
            // Write some byte(s)
            final int cnt = _ttyPortP.writeBytes(bytes, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                      }
            else        { if( tms.timeout() ) return false; }
        }

        // Done
        return true;
    }

    private boolean _writeNCmd_impl(final int numBytes, final int[] bytes, final int[] extraBytes)
    {
        // Prepare the buffer
        final byte[] buf = _buildCmdBytes(numBytes, bytes);
              int    len = buf.length;
              int    ofs = 0;

        // Write the command byte(s)
        _writeNCmd_impl_part(buf);

        // Write the extra bytes
        if(extraBytes != null) _writeNCmd_impl_part( USB2GPIO.ia2ba(extraBytes) );

        // Ensure all the bytes are written
        while( _ttyPortP.bytesAwaitingWrite() > 0 );

        // Done
        return true;
    }

    private boolean _writeNCmd(final int numBytes, final int... bytes)
    { return _writeNCmd_impl(numBytes, bytes, null); }

    private boolean _writeNCmdExt(final int[] extraBytes, final int numBytes, final int... bytes)
    { return _writeNCmd_impl(numBytes, bytes, extraBytes); }

    private boolean _writeCmd(final int... bytes)
    { return _writeNCmd_impl(bytes.length, bytes, null); }

    private boolean _writeCmdExt(final int[] extraBytes, final int... bytes)
    { return _writeNCmd_impl(bytes.length, bytes, extraBytes); }

    private int[] _readRes(final int numBytes, final int[] dstBuff)
    {
        // Prepare the buffer
        final byte[] buf = new byte[numBytes];
              int    len = buf.length;
              int    ofs = 0;

        // Read the response byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS);

        while(len != 0) {
            // Read some byte(s)
            final int cnt = _ttyPortP.readBytes​(buf, len, ofs);
            if(cnt < 0) return null; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                     }
            else        { if( tms.timeout() ) return null; }
        }

        // Convert to integer(s) and return the result
        final int[] res = (dstBuff != null) ? dstBuff : new int[numBytes];

        USB2GPIO.ba2ia(res, buf);

        return res;
    }

    private int[] _readRes(final int numBytes)
    { return _readRes(numBytes, null); }

    private void _readErrorMsg()
    {
        final int[] elen = _readRes(1);

        if(elen == null || elen[0] == 0) return;

        _lastErrorMessage = USB2GPIO.ia2str( _readRes(elen[0]) );

        if(_autoNotifyErrorMessage && _lastErrorMessage != null) USB2GPIO.notifyError(Texts.PDevXXX_SystemError, DevClassNameSystem, _lastErrorMessage);

        /*
        SysUtil.stdDbg().printf("### %s ###\n", _lastErrorMessage);
        //*/
    }

    private boolean _readAck()
    {
        final boolean ack = _checkAck( _readRes(1) );

        if(!ack) _readErrorMsg();
        else     _lastErrorMessage = null;

        return ack;
    }

    private boolean _readNak()
    {
        final boolean nak = _checkNak( _readRes(1) );

        if(nak) _readErrorMsg();
        else    _lastErrorMessage = null;

        return nak;
    }

    public boolean getAutoNotifyErrorMessage()
    { return _autoNotifyErrorMessage; }

    public void setAutoNotifyErrorMessage(final boolean autoNotifyErrorMessage)
    { _autoNotifyErrorMessage = autoNotifyErrorMessage; }

    public String getLastErrorMessage()
    { return _lastErrorMessage; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private SerialPort _ttyPortP  = null;
    private SerialPort _ttyPortS  = null;

    private int[]      _ptVersion = null;
    private int[]      _fwVersion = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int F_CPU              = 16000000;

    private static final int DEFAULT_BAUDRATE_P =  1000000;
    private static final int DEFAULT_BAUDRATE_S =   115200;

    public USB_GPIO(final String primarySerialDevice, final String secondarySerialDevice) throws Exception
    {
        /*
        SysUtil.stdDbg().println( "### Undetermined = " + spiClkFreqToClkDiv(UndeterminedFrequency) );
        SysUtil.stdDbg().println( "### Minimum      = " + spiClkFreqToClkDiv(MinimumFrequency) );
        SysUtil.stdDbg().println( "### Maximum      = " + spiClkFreqToClkDiv(MaximumFrequency) );
        SysUtil.stdDbg().println( "### " );
        SysUtil.stdDbg().println( "### 16000000     = " + spiClkFreqToClkDiv(16000000) );
        SysUtil.stdDbg().println( "###  8000000     = " + spiClkFreqToClkDiv( 8000000) );
        SysUtil.stdDbg().println( "###  6000000     = " + spiClkFreqToClkDiv( 6000000) );
        SysUtil.stdDbg().println( "###  4000000     = " + spiClkFreqToClkDiv( 4000000) );
        SysUtil.stdDbg().println( "###      500     = " + spiClkFreqToClkDiv(     500) );
        SysUtil.stdDbg().println( "###      250     = " + spiClkFreqToClkDiv(     250) );
        SysUtil.stdDbg().println( "###      100     = " + spiClkFreqToClkDiv(     100) );
        SysUtil.stdDbg().println( "### " );
        SysUtil.stdDbg().println( "### USRT Minimum   : " + usrtGetMinimumBaudrate() );
        SysUtil.stdDbg().println( "### USRT Maximum   : " + usrtGetMaximumBaudrate() );
        SysUtil.stdDbg().println( "### USRT Standards : " + Arrays.toString( usrtGetStandardBaudrates() ) );
        SysUtil.stdDbg().println( "### " );
        SysUtil.stdDbg().println( "### UART Minimum   : " + uartGetMinimumBaudrate() );
        SysUtil.stdDbg().println( "### UART Maximum   : " + uartGetMaximumBaudrate() );
        SysUtil.stdDbg().println( "### UART Standards : " + Arrays.toString( uartGetStandardBaudrates() ) );
        SysUtil.stdDbg().println( "### " );
        //*/

        // Open the primary device
        _ttyPortP = SerialPort.getCommPort(primarySerialDevice);

        _ttyPortP.setFlowControl​    (SerialPort.FLOW_CONTROL_DISABLED);
        _ttyPortP.setBaudRate​       (DEFAULT_BAUDRATE_P);
        _ttyPortP.setNumDataBits​    (8);
        _ttyPortP.setParity​         (SerialPort.NO_PARITY);
        _ttyPortP.setNumStopBits​    (SerialPort.ONE_STOP_BIT);
        _ttyPortP.setComPortTimeouts​(SerialPort.TIMEOUT_NONBLOCKING, 1000, 1000);
        _ttyPortP.openPort          ();

        // Open the secondary device
        if(secondarySerialDevice == null) return;

        _ttyPortS = SerialPort.getCommPort(secondarySerialDevice);

        _ttyPortS.setFlowControl​    (SerialPort.FLOW_CONTROL_DISABLED);
        _ttyPortS.setBaudRate​       (DEFAULT_BAUDRATE_S);
        _ttyPortS.setNumDataBits​    (8);
        _ttyPortS.setParity​         (SerialPort.NO_PARITY);
        _ttyPortS.setNumStopBits​    (SerialPort.ONE_STOP_BIT);
        _ttyPortS.setComPortTimeouts​(SerialPort.TIMEOUT_NONBLOCKING, 1000, 1000);
        _ttyPortS.openPort          ();

        /*
        final Thread a = new Thread( () -> {
             SysUtil.stdDbg().println("### A ###");
        } );
        final Thread b = new Thread( () -> {
             SysUtil.stdDbg().println("### B ###");
        } );

        PCF8574_ShutdownHook.register  (a);
        PCF8574_ShutdownHook.register  (b);
        PCF8574_ShutdownHook.unregister(a);
        PCF8574_ShutdownHook.unregister(b);
        //*/
    }

    @Override
    public SerialPort rawSerialPort()
    { return _ttyPortS; }

    @Override
    public void shutdown()
    {
        if(_pcf8574Initialized) {
             pcf8574Disable();
            _pcf8574UninitTWI();
        }

                              _ttyPortP.closePort();
        if(_ttyPortS != null) _ttyPortS.closePort();

        _ptVersion = null;
        _fwVersion = null;
    }

    @Override
    public void resetAndShutdown()
    {
        _reset_impl();
        shutdown();

        SysUtil.sleepMS(1000);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : If more than one 'JxMake USB-GPIO' module is connected to the same external USB hub, the
    //           detection process may not be too reliable (depending on the hub make and model)!

    private static USB_GPIO _autoConnectNth_impl(final int pairIndex_, final boolean uxrtForceDisable)
    {
        // List all serial ports
        final ArrayList<String> serialPorts = SerialPortUtil.listSerialPort();
        final ArrayList<String> blacklist   = new ArrayList<>();

        // Find the primary serial port
        String primarySerialDevice   = null;
        String secondarySerialDevice = null;
        int    pairIndex             = pairIndex_;

        for(final String p : serialPorts) {

            // Check if it has been blacklisted
            if( blacklist.contains(p) ) continue;

            /*
            SysUtil.stdDbg().printf("TESTING PRIMARY   : %s\n", p);
            //*/

            // Create a new 'JxMake USB-GPIO' module device only using the primary serial port
            USB_GPIO dev = null;

            try                     { dev = new USB_GPIO(p, null); }
            catch(final Exception e){ dev = null;                  }

            if(dev == null) {
                /*
                SysUtil.stdDbg().printf("                    ERROR\n");
                //*/
                continue;
            }

            // Send CMD_DETECT
            int[] res = null;

            while(true) {
                res = dev.__detect();
                if(res != null) break;
                if(!uxrtForceDisable) break;
                /*
                SysUtil.stdDbg().printf("                    uxrtForceDisable\n");
                //*/
                if( !dev.hwuartDisable() && !dev.hwusrtDisable() ) break;
            }

            // Shutdown the 'JxMake USB-GPIO' module device
            dev.shutdown();

            // Skip this port if CMD_DETECT was not successful
            if(res == null) continue;

            /*
            for(final int r : res) SysUtil.stdDbg().printf("CMD_DETECT[n] = %02X\n", r);
            //*/

            // Find the secondary serial port
            for(final String s : serialPorts) {

                // Skip if it is the same as the primary serial port
                if( s.equals(p) ) continue;

                // Check if it has been blacklisted
                if( blacklist.contains(s) ) continue;

                /*
                SysUtil.stdDbg().printf("TESTING SECONDARY : %s\n", s);
                //*/

                // Open the secondary serial port
                final SerialPort sp = SerialPort.getCommPort(s);

                sp.setComPortTimeouts​(SerialPort.TIMEOUT_NONBLOCKING, 1000, 1000);
                sp.openPort          ();

                // Send one byte and get the response from the secondary serial port
                final byte[]  chk = new byte[res.length];
                      int     len = 0;
                      boolean got = false;

                if( sp.writeBytes(chk, res.length, 0) == res.length ) {
                    // Ensure all the bytes are written
                    while( sp.bytesAwaitingWrite() > 0 );
                    // Clear all the buffers
                    sp.flushIOBuffers();
                    // Wait for a while
                    SysUtil.sleepMS(500);
                    // Read and check the response
                    len = sp.readBytes​(chk, chk.length, 0);
                    got = (len == chk.length);
                }
                else {
                    /*
                    SysUtil.stdDbg().printf("                    error write\n");
                    //*/
                }

                // Close the port
                sp.closePort();

                /*
                SysUtil.stdDbg().printf("                    %b (%d/%d)\n", got, len, chk.length);
                //*/

                if(!got) continue;

                /*
                for(final byte b : chk) SysUtil.stdDbg().printf("%02X\n", b & 0xFF);
                SysUtil.stdDbg().println();
                //*/

                // Check if the response equals to the one from the primary serial port
                if( Arrays.equals( res, ba2ia(chk) ) ) {
                    // Skip as needed
                    if(pairIndex > 0) {
                        pairIndex--;
                        blacklist.add(p);
                        blacklist.add(s);
                    }
                    // Store the device names
                    else {
                        primarySerialDevice   = p;
                        secondarySerialDevice = s;
                    }
                    break;
                }

            } // for

            // Check if the valid port pair has been found
            if(primarySerialDevice != null && secondarySerialDevice != null) break;

        } // for

        // Create a new 'JxMake USB-GPIO' module device only using both the primary and secondary serial port
        if(primarySerialDevice != null && secondarySerialDevice != null) {

            try {

                final USB_GPIO dev = new USB_GPIO(primarySerialDevice, secondarySerialDevice);

                /*
                SysUtil.stdDbg().println(primarySerialDevice  );
                SysUtil.stdDbg().println(secondarySerialDevice);
                SysUtil.stdDbg().println();
                //*/

                // Test ping
                if( !dev.ping() ) {
                    dev.shutdown();
                    return null; // Invalid pair
                }

                /*
                SysUtil.stdDbg().println(primarySerialDevice  );
                SysUtil.stdDbg().println(secondarySerialDevice);
                SysUtil.stdDbg().println();
                //*/

                // Return the device
                return dev;

            }
            catch(final Exception e) {}

        } // if

        // No valid port pair was found
        return null;
    }

    public static USB_GPIO autoConnectNth(final int pairIndex_)
    {
        final USB_GPIO dev = _autoConnectNth_impl(pairIndex_, false);

        if(dev != null) return dev;

        return _autoConnectNth_impl(pairIndex_, true);
    }

    public static USB_GPIO autoConnectFirst()
    { return autoConnectNth(0); }

    public static USB_GPIO autoConnectSecond()
    { return autoConnectNth(1); }

    public static USB_GPIO autoConnectThird()
    { return autoConnectNth(2); }

    public static boolean isHWv1(final USB2GPIO usb2gpio)
    {
        final USB_GPIO usb_gpio = (usb2gpio instanceof USB_GPIO) ? ( (USB_GPIO) usb2gpio ) : null;

        return (usb_gpio != null) ? usb_gpio.isHWv1() : false;
    }

    public static boolean isHWv2(final USB2GPIO usb2gpio)
    {
        final USB_GPIO usb_gpio = (usb2gpio instanceof USB_GPIO) ? ( (USB_GPIO) usb2gpio ) : null;

        return (usb_gpio != null) ? usb_gpio.isHWv2() : false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean invalidCommand()
    {
        if( !_writeCmd(CMD_INVALID) ) return false;

        return _readAck();
    }

    public boolean ping()
    {
        if( !_writeCmd(CMD_PING) ) return false;

        return _readAck();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public int[] getProtocolVersion(final boolean alwaysReread)
    {
        if(!alwaysReread && _ptVersion != null) return _ptVersion;

        if( !_writeCmd(CMD_GET_PROTOCOL_VERSION) ) return null;

        if( !_readAck() ) return null;

        _ptVersion = _readRes(4);

        return _ptVersion;
    }

    public int[] getProtocolVersion()
    { return getProtocolVersion(false); }

    public int[] getFirmwareVersion(final boolean alwaysReread)
    {
        if(!alwaysReread && _fwVersion != null) return _fwVersion;

        if( !_writeCmd(CMD_GET_FIRMWARE_VERSION) ) return null;

        if( !_readAck() ) return null;

        _fwVersion = _readRes(4);

        return _fwVersion;
    }

    public int[] getFirmwareVersion()
    { return getFirmwareVersion(false); }

    public int[] getVersion(final boolean alwaysReread)
    {
        if( getProtocolVersion(alwaysReread) == null ) return null;
        if( getFirmwareVersion(alwaysReread) == null ) return null;

        return XCom.arrayConcatCopy(_ptVersion, _fwVersion);
    }

    public int[] getVersion()
    { return getVersion(false); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _isPv1   () { return getProtocolVersion()[0] == 1; }
    private boolean _isPv2   () { return getProtocolVersion()[0] == 2; }
    private int     _getPvIdx() { return getProtocolVersion()[0] - 1 ; }

    public  boolean isHWv1   () { return _isPv1(); }
    public  boolean isHWv2   () { return _isPv2(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean enableDebugMessage(final boolean error, final boolean warning, final boolean notice, final boolean information)
    {
        int mask = 0x00;

        if(error      ) mask |= 0x01;
        if(warning    ) mask |= 0x02;
        if(notice     ) mask |= 0x04;
        if(information) mask |= 0x08;

        if( !_writeCmd(CMD_ENABLE_DEBUG_MESSAGE, mask) ) return false;

        return _readAck();
    }

    private boolean _reset_impl()
    {
        if( !_writeCmd(CMD_RESET) ) return false;

        return _readAck();
    }

    public boolean reset()
    {
        // Reset and shutdown
        if( !_reset_impl() ) return false;
        shutdown();

        // Wait for a while
        SysUtil.sleepMS(1000);

        // Reopen the port(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(REOPEN_PORT_TIMEOUT_MS);

        while( !_ttyPortP.openPort() ) {
            if( tms.timeout() ) {
                shutdown();
                return false;
            }
        }

        if(_ttyPortS != null) _ttyPortS.openPort();

        // Done
        return true;
    }

    public boolean resetToBootloader()
    {
        if( !_writeCmd(CMD_RESET_TO_BOOTLOADER) ) return false;

        if( !_readAck() ) return false;

        shutdown();

        return true;
    }

    public int[] __detect()
    {
        // "\rhELLo\n"
        if( !_writeCmd(CMD_DETECT, 'h', 'E', 'L', 'L', 'o', '\n') ) return null;

        if( !_readAck() ) return null;

        final int[] res = _readRes(32);

        // "JxMake USB-GPIO Module\n"
        if(res[ 0] != 0x4A || res[ 1] != 0x78 || res[ 2] != 0x4D || res[ 3] != 0x61 || res[ 4] != 0x6B || res[ 5] != 0x65 ||
           res[ 6] != 0x20 || res[ 7] != 0x55 || res[ 8] != 0x53 || res[ 9] != 0x42 || res[10] != 0x2D || res[11] != 0x47 ||
           res[12] != 0x50 || res[13] != 0x49 || res[14] != 0x4F || res[15] != 0x20 || res[16] != 0x4D || res[17] != 0x6F ||
           res[18] != 0x64 || res[19] != 0x75 || res[20] != 0x6C || res[21] != 0x65 || res[22] != 0x0A
        ) {
           return null;
        }

        return new int[] {
            res[23], res[24], res[25], res[26], res[27], res[28], res[29], res[30], res[31]
        };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean hwgpioSetAllHighImpedance()
    {
        if( !_writeCmd(CMD_HW_GPIO_SET_MODE, 0x00, 0x00, 0x00) ) return false;

        return _readAck();
    }

    public boolean hwgpioSetMode(final int[] gpioModes, final boolean[] gpioIVals)
    {
        // Generate the command arguments
        int modeH = 0;
        int modeL = 0;
        int inVal = 0;

        for(int i = 4; i < 8; ++i) {
            modeH |= ( gpioModes[i] & 0x03 ) << ( (i - 4) * 2);
            if(gpioIVals[i]) inVal |= (1 << i);
        }

        for(int i = 0; i < 4; ++i) {
            modeL |= ( gpioModes[i] & 0x03 ) << (  i      * 2);
            if(gpioIVals[i]) inVal |= (1 << i);
        }

        // Send the command and its arguments
        if( !_writeCmd(CMD_HW_GPIO_SET_MODE, modeH, modeL, inVal) ) return false;

        return _readAck();
    }

    public boolean hwgpioSetValues(final int[] gpioValues)
    {
        // Generate the command arguments
        int mask  = 0;
        int value = 0;

        for(int i = 0; i < 8; ++i) {
            if(gpioValues[i] >= 0) mask  |= (1 << i);
            if(gpioValues[i] >  0) value |= (1 << i);
        }

        // Send the command and its arguments
        if( !_writeCmd(CMD_HW_GPIO_SET_VALUES, mask, value) ) return false;

        return _readAck();
    }

    public boolean[] hwgpioGetValues()
    {
        // Send and check the command
        if( !_writeCmd(CMD_HW_GPIO_GET_VALUES) ) return null;

        if( !_readAck() ) return null;

        // Read and check the response data
        final int[] bits = _readRes(1);

        if(bits == null) return null;

        // Store and return the result
        boolean[] value = new boolean[8];

        for(int i = 0; i < 8; ++i) {
            value[i] = ( ( bits[0] & (1 << i) ) != 0 );
        }

        return value;
    }

    public boolean hwgpioSetPWM(final int gpioNumber, final int pwmValue)
    {
        if( !_writeCmd( CMD_HW_GPIO_SET_PWM, gpioNumber, pwmValue & 0xFF ) ) return false;

        return _readAck();
    }

    public int hwgpioGetADC(final int gpioNumber)
    {
        if( !_writeCmd( CMD_HW_GPIO_GET_ADC, gpioNumber ) ) return -1;

        if( !_readAck() ) return -1;

        final int[] res = _readRes(2);

        if(res == null) return -1;

        return (res[0] << 8) | res[1];
    }

    public int hwgpioGetVREAD()
    {
        if( !isHWv2() ) return -1; // [!!! v2 Protocol ONLY !!!]

        if( !_writeCmd( CMD_HW_GPIO_GET_VREAD ) ) return -1;

        if( !_readAck() ) return -1;

        final int[] res = _readRes(2);

        if(res == null) return -1;

        return (res[0] << 8) | res[1];
    }

    public boolean hwgpioCalibrateVREAD(final int expectedValue)
    {
        if( !isHWv2() ) return false; // [!!! v2 Protocol ONLY !!!]

        if( !_writeCmd( CMD_HW_GPIO_CALIBRATE_VREAD, (expectedValue >> 8) & 0xFF, expectedValue & 0xFF ) ) return false;

        if( !_readAck() ) return false;

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean hwuartEnable()
    {
        if( !_writeCmd(CMD_HW_UART_ENABLE) ) return false;

        return _readAck();
    }

    public boolean hwuartDisable()
    {
        if( !_writeCmd(CMD_HW_UART_DISABLE) ) return false;

        return _readAck();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean hwusrtEnable(final SSMode ssMode, final boolean useXMegaPDIMode)
    {
        int initFlag = 0;

        switch(ssMode) {
            case ActiveLow  : initFlag |= 0x00; break;
            case ActiveHigh : initFlag |= 0x01; break;
        }

        if(useXMegaPDIMode) initFlag |= 0x80;

        if( !_writeCmd(CMD_HW_USRT_ENABLE, initFlag) ) return false;

        return _readAck();
    }

    public boolean hwusrtEnable(final SSMode ssMode)
    { return hwusrtEnable(ssMode, false); }

    public boolean hwusrtEnable()
    { return hwusrtEnable(SSMode.ActiveLow, false); }

    public boolean hwusrtDisable()
    {
        if( !_writeCmd(CMD_HW_USRT_DISABLE) ) return false;

        return _readAck();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _initializedSPI  = false; // A flag that indicates if the system has been initialized
    private boolean _initializedUXRT = false; // ---
    private boolean _initializedSWIM = false; // ---
    private boolean _initializedJTAG = false; // ---

    private boolean _slaveSelected   = false; // A flag that indicates if the slave  has been selected

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean spiIsImplModeSupported(final ImplMode implMode)
    {
        // Only hardware mode is supported
        return implMode == ImplMode.Hardware;
    }

    @Override
    public boolean spiSetImplMode(final ImplMode implMode)
    {
        // Error if already initialized
        if(_initializedSPI) return false;

        // Only hardware mode is supported
        return implMode == ImplMode.Hardware;
    }

    @Override
    public ImplMode spiGetImplMode()
    { return ImplMode.Hardware; }

    @Override
    public boolean spiIsOperationalModeSupported(final OperationalMode operationalMode)
    {
        // Only master mode is supported
        return operationalMode == OperationalMode.Master;
    }

    @Override
    public boolean spiIsDuplexModeSupported(final DuplexMode duplexMode)
    {
        // Only full duplex mode is supported
        return duplexMode == DuplexMode.Full;
    }

    // !!! NOTE : Always synchronize with the protocol manual and firmware implementation !!!
    @Override
    public int[] spiGetSupportedClkDivs()
    {
        if( isHWv2() ) return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        else           return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    }

    /*
     * NOTE : # Only hardware SPI (or hardware-assisted bit-banging SPI) is supported.
     *        # The lower clock frequencies are implemented using hardware-assisted bit-banging SPI
     *          and will have larger deviations (% error).
     *
     * !!! NOTE : Always synchronize with the protocol manual and firmware implementation !!!
     */
    @Override
    public int[] spiGetSupportedClkFreqs()
    {
        if( isHWv2() ) {
            // Implementation : ---------------------------- Hardware SPI ----------------------------    ------ Hardware-assisted bit-banging SPI -------
            return new int[]  { 16000000, 16000000, 8000000, 4000000, 2000000, 1000000, 500000, 250000,   64000, 32000, 16000, 8000, 4000, 2000, 1000, 500 };
            // Clock divider  : 0         1         2        3        4        5        6       7         8      9      10     11    12    13    14    15
        }
        else {
            // Implementation : ---------------------------- Hardware SPI ----------------------------    ------ Hardware-assisted bit-banging SPI -------
            return new int[]  {  8000000,  8000000, 4000000, 2000000, 1000000,  500000, 250000, 125000,   32000, 16000,  8000, 4000, 2000, 1000,  500, 250 };
            // Clock divider  : 0         1         2        3        4        5        6       7         8      9      10     11    12    13     14   15
        }
    }

    @Override public int spiGetFastestHWClkFreq() { return isHWv2() ? 16000000 : 8000000; }
    @Override public int spiGetSlowestHWClkFreq() { return isHWv2() ?   250000 :  125000; }
    @Override public int spiGetFastestBBClkFreq() { return isHWv2() ?    64000 :   32000; }
    @Override public int spiGetSlowestBBClkFreq() { return isHWv2() ?      500 :     250; }

    // !!! NOTE : Always synchronize with the protocol manual and firmware implementation !!!
    @Override
    public int[] spiGetSupportedExtraClkDivs()
    {
        if( isHWv2() ) return new int[] { 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 };
        else           return new int[] { 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111      };
    }

    /*
     * NOTE : These clock frequencies are implemented using hardware-assisted bit-banging SPI
     *        and will have larger deviations (% error).
     *
     * !!! NOTE : Always synchronize with the protocol manual and firmware implementation !!!
     */
    @Override
    public int[] spiGetSupportedExtraClkFreqs()
    {
        if( isHWv2() ) {
            // Implementation : --------------------------- Hardware-assisted bit-banging SPI ----------------------------
            return new int[]  { 280000, 220000, 170000, 150000, 130000, 120000, 100000, 90000, 85000, 80000, 75000, 70000 };
            // Clock divider  : 101     102     103     104     105     106     107     108    109    110    111    112
        }
        else {
            // Implementation : --------------------------- Hardware-assisted bit-banging SPI ----------------------------
            return new int[]  { 160000, 125000, 100000,  90000,  80000,  70000,  60000, 55000, 50000, 45000, 40000        };
            // Clock divider  : 101     102     103     104     105     106     107     108    109    110    111
        }
    }

    @Override public int spiGetFastestExtraBBClkFreq() { return isHWv2() ? 280000 : 160000; }
    @Override public int spiGetSlowestExtraBBClkFreq() { return isHWv2() ?  70000 :  40000; }

    @Override
    public boolean spiBegin(final SPIMode spiMode, final SSMode ssMode, final int clkDiv)
    {
        // Error if already initialized
        if(_initializedSPI) return false;

        // Enable SPI
        int config = 0;

        switch(spiMode) {
            case _0 : config = 0x00 << 5; break;
            case _1 : config = 0x01 << 5; break;
            case _2 : config = 0x02 << 5; break;
            case _3 : config = 0x03 << 5; break;
        }

        switch(ssMode) {
            case ActiveLow  : config |= 0x00 << 4; break;
            case ActiveHigh : config |= 0x01 << 4; break;
        }

        config |= (clkDiv & 0x0F);

        if( !_writeCmd(CMD_HW_SPI_ENABLE, config) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _initializedSPI = true;

        // Done
        return true;
    }

    @Override
    public boolean spiEnd()
    {
        // Error if not initialized
        if(!_initializedSPI) {
            // Force disable
            if( _writeCmd(CMD_HW_SPI_DISABLE) ) _readAck();
            // Return error
            return false;
        }

        // Deselect the slave
        spiDeselectSlave();

        // Disable SPI
        if( !_writeCmd(CMD_HW_SPI_DISABLE) ) return false;

        if( !_readAck() ) return false;

        // Clear flag
        _initializedSPI = false;

        // Done
        return true;
    }

    @Override
    public boolean spiSelectSlave()
    {
        // Error if not initialized or already selected
        if(!_initializedSPI || _slaveSelected) return false;

        // Select the slave
        if( !_writeCmd(CMD_HW_SPI_SELECT_SLAVE) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _slaveSelected = true;

        // Done
        return true;
    }

    @Override
    public boolean spiDeselectSlave()
    {
        // Error if not initialized or not selected
        if(!_initializedSPI || !_slaveSelected) return false;

        // Deselect the slave
        if( !_writeCmd(CMD_HW_SPI_DESELECT_SLAVE) ) return false;

        if( !_readAck() ) return false;

        // Clear flag
        _slaveSelected = false;

        // Done
        return true;
    }

    @Override
    public boolean spiPulseSlaveSelect(final int postDeselectDelayTime_MS, final int postReselectDelayTime_MS)
    {
        // Error if not initialized or not selected
        if(!_initializedSPI || !_slaveSelected) return false;

        // Deselect the slave
        if( !_writeCmd(CMD_HW_SPI_DESELECT_SLAVE) ) return false;

        if( !_readAck() ) return false;

        SysUtil.sleepMS(postDeselectDelayTime_MS);

        // Reselect the slave
        if( !_writeCmd(CMD_HW_SPI_SELECT_SLAVE) ) return false;

        if( !_readAck() ) return false;

        SysUtil.sleepMS(postReselectDelayTime_MS);

        // Done
        return true;
    }

    private int _spiMode2Config(final SPIMode spiMode)
    {
        switch(spiMode) {
            case _0 : return 0x00;
            case _1 : return 0x01;
            case _2 : return 0x02;
            case _3 : return 0x03;
        }

        return -1;
    }

    private boolean _spiTransfer_chunk(final int[] ioBuff)
    {
        // Send the command and data
        if( !_writeCmdExt(ioBuff, CMD_HW_SPI_TRANSFER, ioBuff.length) ) return false;

        if( !_readAck() ) return false;

        // Receive the response
        if( _readRes(ioBuff.length, ioBuff) == null ) return false;

        // Done
        return true;
    }

    @Override
    public boolean spiTransfer(final int[] ioBuff)
    {
        // Error if not initialized or not selected
        if(!_initializedSPI || !_slaveSelected) return false;

        // Perform the transfer
        return spiTransferIgnoreSS(ioBuff);
    }

    @Override
    public boolean spiTransferIgnoreSS(final int[] ioBuff)
    {
        // Error if not initialized
        if(!_initializedSPI) return false;

        // Get the number of chunks to be written
        int numChunks = ioBuff.length / HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ];

        if( ( ioBuff.length % HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) != 0 ) ++numChunks;

        // Write the chunks
        int cchAddr = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Determine the number of byte(s) to be transferred
            final int transLen = Math.min( ioBuff.length - cchAddr, HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ] );

            // Copy the byte(s) to be transferred to a temporary buffer
            final int[] tmpBuff = Arrays.copyOfRange(ioBuff, cchAddr, cchAddr + transLen);

            // Perform SPI transfer
            if( !_spiTransfer_chunk(tmpBuff) ) return false;

            // Copy back the received byte(s) to the main buffer
            for(int i = 0; i < transLen; ++i) ioBuff[cchAddr + i] = tmpBuff[i];

            // Increment the current chunk address
            cchAddr += transLen;

        } // for c

        // Done
        return true;
    }

    @Override
    public boolean spiTransfer_w16Nd_r16dN(final SPIMode spiModeW, final int delayUs25AfterW, final int sizeW, final SPIMode spiModeR, final int delayUs10InterR, final int dummyValueR, final int[] ioBuff)
    {
        // Error if not initialized or not selected
        if(!_initializedSPI || !_slaveSelected) return false;

        // Perform the transfer
        return spiTransferIgnoreSS_w16Nd_r16dN(spiModeW, delayUs25AfterW, sizeW, spiModeR, delayUs10InterR, dummyValueR, ioBuff);
    }

    @Override
    public boolean spiTransferIgnoreSS_w16Nd_r16dN(final SPIMode spiModeW, final int delayUs25AfterW, final int sizeW, final SPIMode spiModeR, final int delayUs10InterR, final int dummyValueR, final int[] ioBuff)
    {
        // Error if not initialized
        if(!_initializedSPI) return false;

        // Get the read size
        final int sizeR = ioBuff.length - sizeW;

        // Error if the number of bytes to be written or read is not even
        if(sizeW % 2 != 0 || sizeR % 2 != 0) return false;

        // Get the number of chunks to be written and read
        int numChunksW = sizeW / HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ];
        int numChunksR = sizeR / HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ];

        if( ( sizeW % HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) != 0 ) ++numChunksW;
        if( ( sizeR % HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) != 0 ) ++numChunksR;

        // Determine the configuration value
        final int configW = ( (delayUs25AfterW & 0x0F) << 4 ) | _spiMode2Config(spiModeW);
        final int configR = ( (delayUs10InterR & 0x0F) << 4 ) | _spiMode2Config(spiModeR);

        // Write and read the chunks
        int cchAddrW = 0;
        int cchAddrR = 0;

        for(int c = 0; c < numChunksW; ++c) {

            // Determine the number of byte(s) to be transferred
            final int transLenW = Math.min( sizeW - cchAddrW, HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ] );

            // Copy the byte(s) to be transferred to a temporary buffer
            final int[] tmpBuffW = Arrays.copyOfRange(ioBuff, cchAddrW, cchAddrW + transLenW);

            // Perform SPI transfer
            if(c < numChunksW - 1) {
                if( !_writeCmdExt(tmpBuffW, CMD_HW_SPI_TRANSFER_W16ND_R16DN, configW, tmpBuffW.length, configR, 0        , dummyValueR) ) return false;
            }
            else {
                final int transLenR = Math.min( sizeR - cchAddrR, HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ] );
                if( !_writeCmdExt(tmpBuffW, CMD_HW_SPI_TRANSFER_W16ND_R16DN, configW, tmpBuffW.length, configR, transLenR, dummyValueR) ) return false;
            }

            if( !_readAck() ) return false;

            // Increment the current chunk address
            cchAddrW += transLenW;

        } // for c

        for(int c = 0; c < numChunksR; ++c) {

            // Determine the number of byte(s) to be transferred
            final int transLenR = Math.min( sizeR - cchAddrR, HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ]);

            // Copy the byte(s) to be transferred to a temporary buffer
            final int[] tmpBuffR = new int[transLenR];

            // Perform SPI transfer
            if(c > 0 || sizeW == 0) {
                if( !_writeCmd(CMD_HW_SPI_TRANSFER_W16ND_R16DN, configW, 0, configR, transLenR, dummyValueR) ) return false;
                if( !_readAck() ) return false;
            }

            if( _readRes(tmpBuffR.length, tmpBuffR) == null ) return false;

            // Copy back the received byte(s) to the main buffer
            for(int i = 0; i < transLenR; ++i) ioBuff[sizeW + cchAddrR + i] = tmpBuffR[i];

            // Increment the current chunk address
            cchAddrR += transLenR;

        } // for c

        // Done
        return true;
    }

    @Override
    public boolean spiSetClkDiv(final int clkDiv)
    {
        // Error if not initialized
        if(!_initializedSPI) return false;

        // Set the SPI clock frequency
        if( !_writeCmd(CMD_HW_SPI_SET_SCK_FREQUENCY, clkDiv & 0x7F) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    @Override
    public boolean spiSetSPIMode(final SPIMode spiMode)
    {
        // Error if not initialized
        if(!_initializedSPI) return false;

        // Set the SPI mode
        final int config = _spiMode2Config(spiMode);

        if( !_writeCmd(CMD_HW_SPI_SET_SPI_MODE, config) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    @Override
    public boolean spiSetBreak(final boolean mosi, final boolean sclk)
    {
        // Error if not initialized
        if(!_initializedSPI) return false;

        // Set the SPI break
        if( !_writeCmd( CMD_HW_SPI_SET_CLR_BREAK, 0x80 | (mosi ? 0x02 : 0x00) | (sclk ? 0x01 : 0x00) ) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    @Override
    public int spiSetBreakExt(final boolean mosi, final boolean sclk)
    {
        // Error if not initialized
        if(!_initializedSPI) return -1;

        // Set the SPI break
        if( !_writeCmd( CMD_HW_SPI_SET_CLR_BREAK_EXT, 0x80 | (mosi ? 0x02 : 0x00) | (sclk ? 0x01 : 0x00) ) ) return -1;

        if( !_readAck() ) return -1;

        // Receive the response
        final int[] res = _readRes(1);

        if(res == null) return -1;

        // Done
        return res[0];
    }

    @Override
    public boolean spiClrBreak()
    {
        // Error if not initialized
        if(!_initializedSPI) return false;

        // Clear the SPI break
        if( !_writeCmd(CMD_HW_SPI_SET_CLR_BREAK, 0x00) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    @Override
    public boolean spiXBTransfer(final IEVal iMOSI, final IEVal iSCLK, final int iDelayMS, final IEVal eMOSI, final IEVal eSCLK, final int[] ioBuff)
    {
        // Error if not initialized or not selected
        if(!_initializedSPI || !_slaveSelected) return false;

        // Perform the transfer
        return spiXBTransferIgnoreSS(iMOSI, iSCLK, iDelayMS, eMOSI, eSCLK, ioBuff);
    }

    private boolean _spiXBTransferIgnoreSS_impl(final IEVal iMOSI, final IEVal iSCLK, final int iDelayMS, final IEVal eMOSI, final IEVal eSCLK, final int[] ioBuff)
    {
        // Prepare the configuration bits
        int ioocc_eooc = 0;
        int vvvvvvvv   = iDelayMS;

             if(iMOSI == IEVal._I) ioocc_eooc |= 0b10000000;
        else if(iMOSI == IEVal._A) ioocc_eooc |= 0b11000000;
             if(iSCLK == IEVal._I) ioocc_eooc |= 0b00100000;
        else if(iSCLK == IEVal._A) ioocc_eooc |= 0b00110000;
             if(eMOSI == IEVal._I) ioocc_eooc |= 0b00001000;
        else if(eMOSI == IEVal._A) ioocc_eooc |= 0b00001100;
             if(eSCLK == IEVal._I) ioocc_eooc |= 0b00000010;
        else if(eSCLK == IEVal._A) ioocc_eooc |= 0b00000011;

             if(vvvvvvvv <   0) vvvvvvvv =   0;
        else if(vvvvvvvv > 255) vvvvvvvv = 255;

        // Send the command and data
        if( !_writeCmdExt(ioBuff, CMD_HW_SPI_XB_TRANSFER, ioocc_eooc, vvvvvvvv, ioBuff.length / 2) ) return false;

        if( !_readAck() ) return false;

        // Receive the response
        if( _readRes(ioBuff.length, ioBuff) == null ) return false;

        // Done
        return true;
    }

    @Override
    public boolean spiXBTransferIgnoreSS(final IEVal iMOSI, final IEVal iSCLK, final int iDelayMS, final IEVal eMOSI, final IEVal eSCLK, final int[] ioBuff)
    {
        // Error if not initialized
        if(!_initializedSPI) return false;

        // Error if the number of bytes is not even
        if( (ioBuff.length % 2) != 0 ) return false;

        // Get the number of chunks to be written
        final int maxSize   = ( HW_SPI_TRANSFER_MAX_SIZE[ _getPvIdx() ] - 4 ) / 4 * 2;
              int numChunks = ioBuff.length / maxSize;

        if( (ioBuff.length % maxSize) != 0 ) ++numChunks;

        // Check if there is only one chunk
        if(numChunks == 1) return _spiXBTransferIgnoreSS_impl(iMOSI, iSCLK, iDelayMS, eMOSI, eSCLK, ioBuff);

        // Write the chunks
        int cchAddr = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Determine the number of byte(s) to be transferred
            final int transLen = Math.min(ioBuff.length - cchAddr, maxSize);

            // Copy the byte(s) to be transferred to a temporary buffer
            final int[] tmpBuff = Arrays.copyOfRange(ioBuff, cchAddr, cchAddr + transLen);

            // Perform SPI transfer
                 if(c == 0            ) { if( !_spiXBTransferIgnoreSS_impl(iMOSI   , iSCLK   , iDelayMS, IEVal._X, IEVal._X, tmpBuff) ) return false; }
            else if(c == numChunks - 1) { if( !_spiXBTransferIgnoreSS_impl(IEVal._X, IEVal._X, 0       , eMOSI   , eSCLK   , tmpBuff) ) return false; }
            else                        { if( !_spiXBTransferIgnoreSS_impl(IEVal._X, IEVal._X, 0       , IEVal._X, IEVal._X, tmpBuff) ) return false; }

            // Copy back the received byte(s) to the main buffer
            for(int i = 0; i < transLen; ++i) ioBuff[cchAddr + i] = tmpBuff[i];

            // Increment the current chunk address
            cchAddr += transLen;

        } // for c

        // Done
        return true;
    }

    @Override
    public boolean spiXBSpecial(final int type)
    {
        // Error if not initialized
        if(!_initializedSPI) return false;

        // Send the command
        if( !_writeCmd(CMD_HW_SPI_XB_SPECIAL, type) ) return false;

        if( !_readAck() ) return false;

        // Receive the response
        final int[] res = _readRes(1);

        if(res == null) return false;

        // Set/clear flag
        _slaveSelected = (res[0] != 0);

        // Done
        return true;
    }

    public boolean spiXBSpecial_dsPIC30_HVICSP_EntrySequence()
    { return spiXBSpecial(HW_SPI_XBS_DSPIC30_HV_ESQ); }

    public boolean spiXBSpecial_dsPIC33_LVX_EntrySequence()
    { return spiXBSpecial(HW_SPI_XBS_DSPIC33_LV_ESQ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int HWTWI_DEFAULT_TIMEOUT_MS = 10;

    private boolean _hwtwiInitialized = false; // A flag that indicates if the system has been initialized

    public boolean hwtwiBegin(final int sclFrequency, final int timeoutMS_, final boolean enableExternalPullUpResistors)
    {
        // ##### !!! TODO : 'enableExternalPullUpResistors is [!!! v1 Protocol ONLY !!!] - Add note? !!! #####

        // Error if already initialized
        if(_hwtwiInitialized) return false;

        // Prepare the configuration bits
        final int timeoutMS      = (timeoutMS_ & 0x0F);

              int configuration  = enableExternalPullUpResistors ? 0x01 : 0x00;
                  configuration |= ( (timeoutMS <= 0) ? HWTWI_DEFAULT_TIMEOUT_MS : timeoutMS ) << 4;

        // Enable TWI
        if( !_writeCmd(
            CMD_HW_TWI_ENABLE, (sclFrequency >> 16) & 0xFF, (sclFrequency >> 8) & 0xFF, sclFrequency & 0xFF, configuration
        ) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _hwtwiInitialized = true;

        // Done
        return true;
    }

    public boolean hwtwiEnd()
    {
        // Error if not initialized
        if(!_hwtwiInitialized) {
            // Force disable
            if( _writeCmd(CMD_HW_TWI_DISABLE) ) _readAck();
            // Return error
            return false;
        }

        // Send the command
        if( !_writeCmd(CMD_HW_TWI_DISABLE) ) return false;

        if( !_readAck() ) return false;

        // Clear flag
        _hwtwiInitialized = false;

        // Done
        return true;
    }

    public boolean hwtwiWrite(final int slaveAddress, final int[] buff, final boolean sendStop)
    {
        // Error if not initialized
        if(!_hwtwiInitialized ) return false;

        // Check the buffer size
        if( buff.length > HW_TWI_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) return false;

        // Send the command and data
        if( !_writeCmdExt(buff, sendStop ? CMD_HW_TWI_WRITE : CMD_HW_TWI_WRITE_NO_STOP, slaveAddress, buff.length) ) return false;

        return _readAck();
    }

    public boolean hwtwiWrite(final int slaveAddress, final int[] buff)
    { return hwtwiWrite(slaveAddress, buff, true); }

    public boolean hwtwiRead(final int slaveAddress, final int[] buff, final boolean sendStop)
    {
        // Error if not initialized
        if(!_hwtwiInitialized ) return false;

        // Check the buffer size
        if( buff.length > HW_TWI_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) return false;

        // Send the command and data
        if( !_writeCmd(sendStop ? CMD_HW_TWI_READ : CMD_HW_TWI_READ_NO_STOP, slaveAddress, buff.length) ) return false;

        if( !_readAck() ) return false;

        // Read the response
        return _readRes(buff.length, buff) != null;
    }

    public boolean hwtwiRead(final int slaveAddress, final int[] buff)
    { return hwtwiRead(slaveAddress, buff, true); }

    public int[] hwtwiScan()
    {
        // Error if not initialized
        if(!_hwtwiInitialized ) return null;

        // [!!! v2 Protocol ONLY !!!]
        if( !isHWv2() ) return null;

        // Send the command
        if( !_writeCmd(CMD_HW_TWI_SCAN) ) return null;

        final XCom.TimeoutMS tms = new XCom.TimeoutMS(128 * 15); // The maximum time out is 128 * 15mS

        while( !_readAck() ) {
            if( tms.timeout() ) return null;
        }

        // Read the response
        final int[] result128 = new int[128];

        return ( _readRes(result128.length, result128) != null ) ? result128 : null;
    }

    public boolean hwtwiWriteOneCF(final int sclFrequency, final int slaveAddress, final int data)
    {
        // Error if not initialized
        if(!_hwtwiInitialized ) return false;

        // [!!! v2 Protocol ONLY !!!]
        if( !isHWv2() ) return false;

        // Send the command and data
        if( !_writeCmd(
            CMD_HW_TWI_WRITE_ONE_CF,
            (sclFrequency >> 16) & 0xFF, (sclFrequency >> 8) & 0xFF, sclFrequency & 0xFF,
            slaveAddress,
            data
        ) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    public boolean hwtwiReadOneCF(final int sclFrequency, final int slaveAddress, final int[] buff)
    {
        // Error if not initialized
        if(!_hwtwiInitialized ) return false;

        // [!!! v2 Protocol ONLY !!!]
        if( !isHWv2() ) return false;

        // Send the command and data
        if( !_writeCmd(
            CMD_HW_TWI_READ_ONE_CF,
            (sclFrequency >> 16) & 0xFF, (sclFrequency >> 8) & 0xFF, sclFrequency & 0xFF,
            slaveAddress
        ) ) return false;

        if( !_readAck() ) return false;

        // Read the response
        return _readRes(1, buff) != null;
    }

    public int hwtwiReadOneCF(final int sclFrequency, final int slaveAddress)
    {
        final int[] buff = new int[1];

        if( !hwtwiReadOneCF(sclFrequency, slaveAddress, buff) ) return -1;

        return buff[0];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : Returns -1 on error, 0 if HW-TWI was NOT enabled, and 1 if HW-TWI was enabled
    private int _hwtwiIsEnabled()
    {
        // NOTE : Do not check '_hwtwiInitialized' here

        // [!!! v2 Protocol ONLY !!!]
        if( !isHWv2() ) return -1;

        // Send the command
        if( !_writeCmd(CMD_HW_TWI_IS_ENABLED) ) return -1;

        if( !_readAck() ) return -1;

        // Read the response
        final int[] result1 = new int[1];

        if( _readRes(result1.length, result1) == null ) return -1;

        // Return the state
        return (result1[0] != 0) ? 1 : 0;
    }

    // NOTE : Returns -1 on error, 0 if HW-TWI was already enabled, and 1 if this call enables HW-TWI
    private int _hwtwiSyncBegin(final int sclFrequency)
    {
        // Error if already initialized
        if(_hwtwiInitialized) return -1;

        // Check if HW-TWI was already enabled
        final int en = _hwtwiIsEnabled();

        if(en <  0) return -1; // Error

        if(en == 1) {          // HW-TWI was already enabled
            // Set flag
            _hwtwiInitialized = true;
            // Done
            return 0;
        }

        // Initialize HW-TWI normally
        return hwtwiBegin(sclFrequency, -1, false) ? 1 : -1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * The following are special functions used to control the relay/solid-state-switch sub-module
     * via the HLF8574/PCF8574/PCF8574A IC, which is mounted on the 'JxMake Versatile MCU Programmer II'
     * module PCB. Communication is established via TWI from the 'JxMake USB-GPIO II' module.
     *
     * PCF/HLF8574(A) Pn     Operational Role
     * (TWI Data Bit #n)
     *
     *         0             Enable TPI                (normal   impedance; mutually exclusive with P1   )
     *         1             Enable TPI                (lower    impedance; mutually exclusive with P0   )
     *         2             Enable PDI                (standard impedance                               )
     *         3             Enable UPDI               (normal   impedance; may be used alone or with P4 )
     *         4             Enable UPDI               (lower    impedance; requires P3 to be enabled    )
     *         5             Enable LGT8/SWD/SWIM/ICSP (680R     impedance; may be combined with P6/P7   )
     *         6             Enable LGT8/SWD/SWIM/ICSP (470R     impedance; may be combined with P5/P7   )
     *         7             Enable LGT8/SWD/SWIM/ICSP (330R     impedance; may be combined with P5/P6   )
     *
     * NOTE : ICSP refers to the In-Circuit Serial Programming interface for Microchip PIC MCUs.
     */

    private static enum PCF8574_Mode {

        DISABLE    (0b00000000),

        TPI_NORMAL (0b00000001), // ≈ 1000R
        TPI_LOWZ   (0b00000010), // ≈  330R

        PDI_STD    (0b00000100), // ≈  220R

        UPDI_NORMAL(0b00001000), // ≈  330R
        UPDI_LOWZ  (0b00011000), // ≈  270R   (P4 requires P3 to be enabled too)

        // NOTE : The mixed-mode entries 'MMODE_*' are for LGT8, SWD, SWIM, and ICSP.
        MMODE_680R (0b00100000), // ≈  680R
        MMODE_470R (0b01000000), // ≈  470R
        MMODE_330R (0b10000000), // ≈  330R
        MMODE_280R (0b01100000), // ≈  280R   (680R || 470R         ≈ 277.91R)
        MMODE_220R (0b10100000), // ≈  220R   (680R || 330R         ≈ 222.18R)
        MMODE_190R (0b11000000), // ≈  190R   (470R || 330R         ≈ 193.88R)
        MMODE_150R (0b11100000); // ≈  150R   (680R || 470R || 330R ≈ 150.86R)

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final int bitmask;

        private PCF8574_Mode(final int bitmask_)
        { bitmask = (~bitmask_) & 0xFF; } // Invert the bits because the signals are active-low

    } // enum PCF8574_Mode

    private static class PCF8574_ShutdownHook {

        private static final    ArrayList<Thread> _shutdownHooks = new ArrayList<Thread>();
        private static volatile boolean           _spshInstalled = false;

        private static void _register_SerialPortShutdownHook()
        {
            if(_spshInstalled) return;

            SerialPort.addShutdownHook( new Thread( () -> {

                if( _shutdownHooks.isEmpty() ) return;

                boolean wasInterrupted = false;

                ArrayList<Thread> hooksCopy;
                synchronized (PCF8574_ShutdownHook.class) {
                    // Synchronize and copy the list to prevent ConcurrentModificationException
                    hooksCopy = new ArrayList<>(_shutdownHooks);
                }

                for(final Thread hook : hooksCopy) {

                    try {
                        hook.start();
                        hook.join();
                    }
                    catch(final InterruptedException ignored) {
                        wasInterrupted = true;
                    }
                    catch(final Exception ignored) {}

                } // for

                if(wasInterrupted) Thread.currentThread().interrupt();

                _shutdownHooks.clear();

            } ) );

            _spshInstalled = true;
        }

        public synchronized static void register(final Thread hook)
        {
            _register_SerialPortShutdownHook();
            _shutdownHooks.add(hook);
        }

        public synchronized static void unregister(final Thread hook)
        { _shutdownHooks.remove(hook); }

    } // class PCF8574_ShutdownHook

    private Thread  _pcf8574ShutdownHook = null;
    private boolean _pcf8574SelfInitTWI  = false;
    private boolean _pcf8574Initialized  = false;

    private int     _pcf8574TWIFrequency = -1;
    private int     _pcf8574TWIAddress   = -1;

    /*
     *  System Name     Nominal   Minimum   Maximum
     *  [5.0V system]   5.0V      4.5V      5.5V
     *  [3.3V system]   3.3V      3.1V      3.5V
     *  [2.5V system]   2.5V      2.4V      2.6V
     *  [1.8V system]   1.8V      1.7V      1.9V
     *  [1.2V system]   1.2V      1.1V      1.3V
     */

    private static boolean _pcf8574_5V0_vtg(final int vtg)
    {
        // VTG is assumed to be ~5.0V (max 5.5V) if the read value is > 3.5V or if a read error occurs
        return (vtg < 0 || vtg > 350);
    }

    private static boolean _pcf8574_3V3_vtg(final int vtg)
    {
        // VTG is assumed to be ~3.3V (max 3.5V) if the read value is > 2.6V and NO read error occurred
        return (vtg > 0 && vtg > 260);
    }

    private static boolean _pcf8574_2V5_vtg(final int vtg)
    {
        // VTG is assumed to be ~2.5V (max 2.6V) if the read value is > 1.9V and NO read error occurred
        return (vtg > 0 && vtg > 190);
    }

    private static boolean _pcf8574_1V8_vtg(final int vtg)
    {
        // VTG is assumed to be ~1.8V (max 1.9V) if the read value is > 1.3V and NO read error occurred
        return (vtg > 0 && vtg > 130);
    }

    private void _pcf8574RegisterShutdownHook()
    {
        if(_pcf8574ShutdownHook != null) return;

        _pcf8574ShutdownHook = new Thread( () -> {
             pcf8574Disable();
            if(_pcf8574SelfInitTWI) hwtwiEnd();
        } );

        PCF8574_ShutdownHook.register(_pcf8574ShutdownHook);
    }

    private void _pcf8574UnregisterShutdownHook()
    {
        if(_pcf8574ShutdownHook == null) return;

        try { PCF8574_ShutdownHook.unregister(_pcf8574ShutdownHook); }
        catch(final Exception e) {}

        _pcf8574ShutdownHook = null;
    }

    private boolean _pcf8574Present()
    { return (_pcf8574TWIFrequency > 0) && (_pcf8574TWIAddress > 0); }

    private boolean _pcf8574InitTWI()
    {
        // Exit if already initialized
        if(_pcf8574Initialized) return true;

        /*
         * Get the HLF8574/PCF8574/PCF8574A frequency and address
         *
         * NOTE : When using the legacy 'JxMake Versatile MCU Programmer' module , set either of the above
         *        environment variables to -1 to prevent the Java driver from using the aforementioned IC.
         */

        _pcf8574TWIFrequency = Integer.decode( SysUtil.getEnv("JXMAKE_PCF8574_FREQUENCY", "100000") );
        _pcf8574TWIAddress   = Integer.decode( SysUtil.getEnv("JXMAKE_PCF8574_ADDRESS"  , "0x27"  ) );

        // Initialize TWI in case it has not been initialized
        if( _pcf8574Present() ) {
            // Prevent error messages from being printed to the console
            USB2GPIO.redirectNotifyErrorToString(true);
            // Silently initialize TWI
            final int res = _hwtwiSyncBegin(_pcf8574TWIFrequency);
            if(res >= 0) {
                // Set the flag as needed
                _pcf8574SelfInitTWI = (res == 1);
            }
            // Allow error messages to be printed to the console
            USB2GPIO.redirectNotifyErrorToString(false);
            // Check for error
            if(res < 0) return false;
        }

        // Set the flag
        _pcf8574Initialized = true;

        // Register the shutdown hook
        if( _pcf8574Present() ) _pcf8574RegisterShutdownHook();

        // Done
        return true;
    }

    private boolean _pcf8574UninitTWI()
    {
        // Exit if not initialized
        if(!_pcf8574Initialized) return false;

        // Only uninitialize if this group of functions initializes the TWI
        if( _pcf8574Present() && _pcf8574SelfInitTWI ) {
            // Prevent error messages from being printed to the console
            USB2GPIO.redirectNotifyErrorToString(true);
            // Silently initialize TWI
            final boolean res = hwtwiEnd();
            // Allow error messages to be printed to the console
            USB2GPIO.redirectNotifyErrorToString(false);
            // Check for error
            if(!res) return false;
        }

        // Clear the flags
        _pcf8574SelfInitTWI = false;
        _pcf8574Initialized = false;

        // Unregister the shutdown hook
        if( _pcf8574Present() ) _pcf8574UnregisterShutdownHook();

        // Done
        return true;
    }

    public boolean pcf8574Disable()
    {
        if( !isHWv2() ) return true; // Silently ignore the command if this is not the 'JxMake USB-GPIO II' module

        if( !_pcf8574InitTWI() ) return false;

        if( !_pcf8574Present() ) return true; // Silently ignore the command if the IC is not present

        return hwtwiWriteOneCF(_pcf8574TWIFrequency, _pcf8574TWIAddress, PCF8574_Mode.DISABLE.bitmask);
    }

    private boolean _pcf8574Enable_impl(final String name, final int vtg, final PCF8574_Mode mode)
    {
        if( !pcf8574Disable() ) return false;

        if( !_pcf8574Present() ) return true; // Silently ignore the command if the IC is not present

        final boolean res = hwtwiWriteOneCF(_pcf8574TWIFrequency, _pcf8574TWIAddress, mode.bitmask);

        SysUtil.stdDbg().printf(
            "\n[PCF8574] [%s] Vtg ≈ %3.2f ⇒ %s.%s : %s\n\n\n",
            name, vtg / 100.0, mode.getClass().getSimpleName(), mode.name(), res ? "✔" : "✖"
        );

        return res;
    }

    /*
     * Current Limits
     *
     *     74AXP2T45   ±25mA
     *     74LVC2T45   ±50mA
     *     74LXC2T45   ±50mA
     *
     *     ATtiny      ±40mA
     *     ATmega      ±40mA
     *     ATxmega     ±25mA
     *
     *     LGT8        ±20mA
     *
     *     PIC10       ±25mA
     *     PIC12       ±25mA
     *     PIC16       ±25mA
     *     PIC18       ±25mA
     *     PIC24       ±25mA
     *     dsPIC30     ±20mA
     *     dsPIC33     ±15mA
     *
     *     STM8        ±20mA   (±8mA in SWIM           mode)
     *
     *     STM32       ±25mA
     *     RP2040      ± 8mA
     *     RP234x      ±20mA
     *     nRF5x       ± 8mA   (±4mA in standard drive mode)
     *     SAMD21      ± 7mA   (±2mA in standard drive mode)
     *     SAMD3x      ± 8mA
     *     RA4Mx       ± 8mA   (±2mA in standard drive mode)
     *
     * NOTE : Most MCUs with low-current SWD pins implement them as open-drain.
     */

    // ##### !!! TODO : Verify the current below !!! #####

    public boolean pcf8574Enable_TPI()
    {
        if( !isHWv2() ) return true; // Silently ignore the command if this is not the 'JxMake USB-GPIO II' module

        final int vtg = hwgpioGetVREAD();

        // [5.0V system] => maximum short-circuit current = 5.0V / 1000R ≈ 5.0mA (±10%) → 4.5mA to 5.5mA
        return _pcf8574Enable_impl("AVR-TPI", vtg, PCF8574_Mode.TPI_NORMAL);

        /* ##### ??? TODO : TPI_LOWZ ??? #####
         * [5.0V system] => maximum short-circuit current = 5.0V / 330R ≈ 15.2mA (±10%) → 13.7mA to 16.7mA
         */
    }

    public boolean pcf8574Enable_PDI()
    {
        if( !isHWv2() ) return true; // Silently ignore the command if this is not the 'JxMake USB-GPIO II' module

        final int vtg = hwgpioGetVREAD();

        // [3.3V system] => maximum short-circuit current = 3.3V / 220R ≈ 15.0mA (±10%) → 13.5mA to 16.5mA
        return _pcf8574Enable_impl("AVR-PDI", vtg, PCF8574_Mode.PDI_STD);
    }

    public boolean pcf8574Enable_UPDI()
    {
        if( !isHWv2() ) return true; // Silently ignore the command if this is not the 'JxMake USB-GPIO II' module

        final String name = "AVR-UPDI";
        final int    vtg  = hwgpioGetVREAD();

        if( _pcf8574_5V0_vtg(vtg) ) {
            // [5.0V system] => maximum short-circuit current = 5.0V / 330R ≈ 15.2mA (±10%) → 13.7mA to 16.7mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.UPDI_NORMAL);
        }
        else if( _pcf8574_3V3_vtg(vtg) || _pcf8574_2V5_vtg(vtg) || _pcf8574_1V8_vtg(vtg) ) {
            // [3.3V system] => maximum short-circuit current = 3.3V / 270R ≈ 12.2mA (±10%) → 11.0mA to 13.4mA
            // [2.5V system] => maximum short-circuit current = 2.5V / 270R ≈  9.3mA (±10%) →  8.4mA to 10.2mA
            // [1.8V system] => maximum short-circuit current = 1.8V / 270R ≈  6.7mA (±10%) →  6.0mA to  7.4mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.UPDI_LOWZ);
        }
        else {
            // Error
            return false;
        }
    }

    private boolean _pcf8574Enable_LGT8_ICSP_impl(final String name)
    {
        if( !isHWv2() ) return true; // Silently ignore the command if this is not the 'JxMake USB-GPIO II' module

        final int vtg = hwgpioGetVREAD();

        if( _pcf8574_5V0_vtg(vtg) ) {
            // [5.0V system] => maximum short-circuit current = 5.0V / 680R ≈ 7.4mA (±10%) → 6.7mA to 8.1mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_680R);
        }
        else if( _pcf8574_3V3_vtg(vtg) ) {
            // [3.3V system] => maximum short-circuit current = 3.3V / 470R ≈ 7.0mA (±10%) → 6.3mA to 7.7mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_470R);
        }
        else if( _pcf8574_2V5_vtg(vtg) ) {
            // [2.5V system] => maximum short-circuit current = 2.5V / 330R ≈ 7.6mA (±10%) → 6.8mA to 8.4mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_330R);
        }
        else if( _pcf8574_1V8_vtg(vtg) ) {
            // [1.8V system] => maximum short-circuit current = 1.8V / 280R ≈ 6.4mA (±10%) → 5.8mA to 7.0mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_280R);
        }
        else {
            // Error
            return false;
        }
    }

    public boolean pcf8574Enable_LGT8()
    { return _pcf8574Enable_LGT8_ICSP_impl("LGT8-SWD"); }

    public boolean pcf8574Enable_ICSP()
    { return _pcf8574Enable_LGT8_ICSP_impl("PIC-ICSP"); }

    public boolean pcf8574Enable_SWIM()
    {
        if( !isHWv2() ) return true; // Silently ignore the command if this is not the 'JxMake USB-GPIO II' module

        final String name = "STM8-SWIM";
        final int    vtg  = hwgpioGetVREAD();

        if( _pcf8574_5V0_vtg(vtg) ) {
            // [5.0V system] => maximum short-circuit current = 5.0V / 680R ≈ 7.4mA (±10%) → 6.7mA to 8.1mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_680R);
        }
        else if( _pcf8574_3V3_vtg(vtg) || _pcf8574_2V5_vtg(vtg) ) {
            // [3.3V system] => maximum short-circuit current = 3.3V / 470R ≈ 7.0mA (±10%) → 6.3mA to 7.7mA
            // [2.5V system] => maximum short-circuit current = 2.5V / 470R ≈ 5.3mA (±10%) → 4.8mA to 5.8mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_470R);
        }
        else if( _pcf8574_1V8_vtg(vtg) ) {
            // [1.8V system] => maximum short-circuit current = 1.8V / 330R ≈ 5.5mA (±10%) → 5.0mA to 6.0mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_330R);
        }
        else {
            // Error
            return false;
        }
    }

    public boolean pcf8574Enable_SWD()
    {
        if( !isHWv2() ) return true; // Silently ignore the command if this is not the 'JxMake USB-GPIO II' module

        final String name = "ARM-SWD";
        final int    vtg  = hwgpioGetVREAD();

        if( _pcf8574_5V0_vtg(vtg) || _pcf8574_3V3_vtg(vtg) ) {
            // [5.0V system] => maximum short-circuit current = 5.0V / 680R ≈ 7.4mA (±10%) → 6.7mA to 8.1mA
            // [3.3V system] => maximum short-circuit current = 3.3V / 680R ≈ 4.9mA (±10%) → 4.4mA to 5.4mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_680R);
        }
        else if( _pcf8574_2V5_vtg(vtg) ) {
            // [2.5V system] => maximum short-circuit current = 2.5V / 470R ≈ 5.3mA (±10%) → 4.8mA to 5.8mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_470R);
        }
        else if( _pcf8574_1V8_vtg(vtg) ) {
            // [1.8V system] => maximum short-circuit current = 1.8V / 330R ≈ 5.5mA (±10%) → 5.0mA to 6.0mA
            return _pcf8574Enable_impl(name, vtg, PCF8574_Mode.MMODE_330R);
        }
        else {
            // Error
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ImplMode _usrtImplMode = ImplMode.Hardware; // Use hardware USRT mode by default

    @Override
    public boolean usrtIsImplModeSupported(final ImplMode implMode)
    {
        // Both bit banging and hardware modes are supported
        return implMode != ImplMode.NotSupported;
    }

    @Override
    public boolean usrtSetImplMode(final ImplMode implMode)
    {
        // Error if already initialized
        if(_initializedUXRT) return false;

        // Check the mode
        if(implMode == ImplMode.NotSupported) return false;

        // Set the mode
        _usrtImplMode = implMode;

        // Done
        return true;
    }

    @Override
    public ImplMode usrtGetImplMode()
    { return _usrtImplMode; }

    @Override
    public boolean usrtIsXMegaPDIModeSupported()
    {
        // Only hardware mode supports ATxmega PDI mode
        return _usrtImplMode == ImplMode.Hardware;
    }

    @Override
    public boolean usrtIsOperationalModeSupported(final OperationalMode operationalMode)
    {
        // Only master mode is supported
        return operationalMode == OperationalMode.Master;
    }

    @Override
    public boolean usrtIsDuplexModeSupported(final DuplexMode duplexMode)
    {
        // Hardware USRT supports both full duplex mode and half duplex mode
        if(_usrtImplMode == ImplMode.Hardware) return true;

        // Bit banging USRT only supports half duplex mode
        return duplexMode == DuplexMode.Half;
    }

    @Override
    public boolean usrtIsPulsingXckSupported()
    {
        // Only bit banging mode support manually pulsing the Xck line on request
        return _usrtImplMode == ImplMode.BitBang;
    }

    @Override
    public boolean usrtIsEnablingDisablingTxSupported()
    {
        // Only hardware mode support manually enabling and disabling the Tx line on request
        return _usrtImplMode == ImplMode.Hardware;
    }

    @Override
    public int usrtGetMinimumBaudrate()
    {
        if(_usrtImplMode != ImplMode.Hardware) return UndeterminedFrequency;

        // ( (F_CPU + baudrate) / (2 * baudrate) - 1 ) <= 4095
        final int min = F_CPU / 8191;
        for(int i = min - 1; i <= min + 1; ++i) {
            if( ( F_CPU / (2 * i) - 1 ) <= 4095 ) return i;
        }
        return UndeterminedFrequency;
    }

    @Override
    public int usrtGetMaximumBaudrate()
    {
        if(_usrtImplMode != ImplMode.Hardware) return UndeterminedFrequency;

        return F_CPU / 2;
    }

    /*
     * NOTE : # Both hardware USRT and bit banging USRT are supported.
     *        # Bit banging USRT cannot guarantee the baudrate.
     *
     * !!! NOTE : Always synchronize with the protocol manual and firmware implementation !!!
     */
    @Override
    public int[] usrtGetStandardBaudrates()
    {
        if(_usrtImplMode != ImplMode.Hardware) return _undeterminedClkFreqs;

        /*
        final int[]                        bs = new int[] { usrtGetMinimumBaudrate(), usrtGetMaximumBaudrate(), 1, 300, 600, 1200, 2400, 4800, 9600, 14400, 19200, 28800, 38400, 57600, 76800, 115200, 230400, 250000, 460800, 500000, 921600, 1000000, 2000000, 3000000, 4000000, 5000000, 6000000, 7000000, 8000000 };
        final java.util.ArrayList<Integer> us = new java.util.ArrayList<Integer>();
        for(final int b : bs) {
            if( b < usrtGetMinimumBaudrate() || b > usrtGetMaximumBaudrate() ) continue;
            final int   ubbr = (F_CPU + b) / (2 * b) - 1;
            if(ubbr < 0) break;
            final int   baud = F_CPU / 2 / (ubbr + 1);
            final float errr  = ( ( (float) baud / (float) b ) - 1.0f ) * 100.0f;
            if(ubbr <= 4095) {
                SysUtil.stdDbg().printf("%7d : %4d (%+5.1f)\n", b, ubbr, errr);
                if( Math.abs(errr) <= 3.0 ) us.add(b);
            }
        }
        SysUtil.stdDbg().println( us.toString() );
        //*/

        return new int[] { 2400, 4800, 9600, 14400, 19200, 28800, 38400, 57600, 76800, 115200, 230400, 250000, 460800, 500000, 1000000, 2000000, 4000000, 8000000 };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean bbusrtBegin(final UXRTMode uxrtMode, final SSMode ssMode, final int baudrate)
    {
        // NOTE : Bit banging USRT mode may not be able to accurately meet the specified baudrate

        // Error if already initialized
        if(_initializedUXRT) return false;

        // Prepare the configuration bits
        int configuration = 0;

        switch(uxrtMode) {
            case _8N1 : configuration = BB_USRT_PARITY_NONE | BB_USRT_STOP_BIT_1; break;
            case _8N2 : configuration = BB_USRT_PARITY_NONE | BB_USRT_STOP_BIT_2; break;
            case _8E1 : configuration = BB_USRT_PARITY_EVEN | BB_USRT_STOP_BIT_1; break;
            case _8E2 : configuration = BB_USRT_PARITY_EVEN | BB_USRT_STOP_BIT_2; break;
            case _8O1 : configuration = BB_USRT_PARITY_ODD  | BB_USRT_STOP_BIT_1; break;
            case _8O2 : configuration = BB_USRT_PARITY_ODD  | BB_USRT_STOP_BIT_2; break;
        }

        switch(ssMode) {
            case ActiveLow  : configuration |= BB_USRT_SS_ACTIVE_LOW ; break;
            case ActiveHigh : configuration |= BB_USRT_SS_ACTIVE_HIGH; break;
        }

        // Enable USRT
        if( !_writeCmd(CMD_BB_USRT_ENABLE, (baudrate >> 16) & 0xFF, (baudrate >> 8) & 0xFF, baudrate & 0xFF, configuration) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _initializedUXRT = true;

        // Done
        return true;
    }

    public boolean bbusrtEnd()
    {
        // Error if not initialized
        if(!_initializedUXRT) {
            // Force disable
            if( _writeCmd(CMD_BB_USRT_DISABLE) ) _readAck();
            // Return error
            return false;
        }

        // Deselect the slave
        bbusrtDeselectSlave();

        // Disable USRT
        if( !_writeCmd(CMD_BB_USRT_DISABLE) ) return false;

        if( !_readAck() ) return false;

        // Clear flag
        _initializedUXRT = false;

        // Done
        return true;
    }

    public boolean bbusrtSelectSlave()
    {
        // Error if not initialized or already selected
        if(!_initializedUXRT || _slaveSelected) return false;

        // Select the slave
        if( !_writeCmd(CMD_BB_USRT_SELECT_SLAVE) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _slaveSelected = true;

        // Done
        return true;
    }

    public boolean bbusrtDeselectSlave()
    {
        // Error if not initialized or not selected
        if(!_initializedUXRT || !_slaveSelected) return false;

        // Select the slave
        if( !_writeCmd(CMD_BB_USRT_DESELECT_SLAVE) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _slaveSelected = false;

        // Done
        return true;
    }

    public boolean bbusrtEnableTx()
    { return false; }

    public boolean bbusrtDisableTx()
    { return false; }

    public boolean bbusrtDisableTxAfter(final int nb)
    { return false; }

    public boolean bbusrtPulseXck(final int count, final boolean txValue)
    {
        // Error if not initialized or not selected
        if(!_initializedUXRT || !_slaveSelected) return false;

        // Select the slave
        if( !_writeCmd(CMD_BB_USRT_PULSE_XCK, count & 0xFF, txValue ? 0x01 : 0x00) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    public boolean bbusrtChangeBaudrate(final int baudrate)
    {
        // WARNING : Bit banging USRT mode does not support this feature!
        return false;
    }

    private boolean _bbusrtTx_chunk(final int[] buff)
    {
        // Send the command and data
        if( !_writeCmdExt(buff, CMD_BB_USRT_TX, buff.length) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    public boolean bbusrtTx(final int[] buff)
    {
        // Error if not initialized or not selected
        if(!_initializedUXRT || !_slaveSelected) return false;

        // Get the number of chunks to be written
        int numChunks = buff.length / BB_USRT_TX_RX_MAX_SIZE[ _getPvIdx() ];

        if( ( buff.length % BB_USRT_TX_RX_MAX_SIZE[ _getPvIdx() ] ) != 0 ) ++numChunks;

        // Write the chunks
        int cchAddr = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Determine the number of byte(s) to be transferred
            final int transLen = Math.min( buff.length - cchAddr, BB_USRT_TX_RX_MAX_SIZE[ _getPvIdx() ] );

            // Copy the byte(s) to be transferred to a temporary buffer
            final int[] tmpBuff = Arrays.copyOfRange(buff, cchAddr, cchAddr + transLen);

            // Perform transmit
            if( !_bbusrtTx_chunk(tmpBuff) ) return false;

            // Increment the current chunk address
            cchAddr += transLen;

        } // for c

        // Done
        return true;
    }

    private int[] _bbusrtRx_chunk(final int numBytes)
    {
        // Send the command and data
        if( !_writeCmd(CMD_BB_USRT_RX, numBytes) ) return null;

        if( !_readAck() ) return null;

        // Receive the response
        return _readRes(numBytes);
    }

    public boolean bbusrtRx(final int[] buff)
    {
        // Error if not initialized or not selected
        if(!_initializedUXRT || !_slaveSelected) return false;

        // Get the number of chunks to be read
        int numChunks = buff.length / BB_USRT_TX_RX_MAX_SIZE[ _getPvIdx() ];

        if( ( buff.length % BB_USRT_TX_RX_MAX_SIZE[ _getPvIdx() ] ) != 0 ) ++numChunks;

        // Read the chunks
        int cchAddr = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Determine the number of byte(s) to be transferred
            final int transLen = Math.min( buff.length - cchAddr, BB_USRT_TX_RX_MAX_SIZE[ _getPvIdx() ] );

            // Perform receive and check for error
            final int[] tmpBuff = _bbusrtRx_chunk(transLen);

            if(tmpBuff == null) return false;

            // Copy the received byte(s) to the main buffer
            for(int i = 0; i < transLen; ++i) buff[cchAddr + i] = tmpBuff[i];

            // Increment the current chunk address
            cchAddr += transLen;

        } // for c

        // Done
        return true;
    }

    public boolean bbusrtTx_discardSerialLoopback(final int[] buff)
    {
        // WARNING : Bit banging USRT mode does not support this feature!
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean hwusrtBegin(final UXRTMode uxrtMode, final SSMode ssMode, final int baudrate, final boolean usePDIMode)
    {
        // Error if already initialized
        if(_initializedUXRT) return false;

        // Apply the settings
        USB2GPIO.ttyPortApplyConfig(_ttyPortS, uxrtMode, baudrate);

        // Enable the HW-USRT
        if( !hwusrtEnable(ssMode, usePDIMode) ) {
            if( !hwusrtDisable(                  ) ) return false;
            if( !hwusrtEnable (ssMode, usePDIMode) ) return false;
        }

        // Set flag
        _initializedUXRT = true;

        // Done
        return true;
    }

    public boolean hwusrtEnd()
    {
        // Error if not initialized
        if(!_initializedUXRT) {
            // Force disable
            hwusrtDisable();
            // Return error
            return false;
        }

        // Restore the default settings
        USB2GPIO.ttyPortApplyConfig(_ttyPortS, UXRTMode._8N1, DEFAULT_BAUDRATE_S);

        // Disable the HW-USRT
        hwusrtDisable();

        // Clear flag
        _initializedUXRT = false;

        // Done
        return true;
    }

    protected boolean _hwusrtSelectSlave_impl(final boolean errorIfAlreadySelected)
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Error if already selected (optional)
        if(errorIfAlreadySelected && _slaveSelected) return false;

        // Select the slave
        if( !_writeCmd(CMD_HW_USRT_SELECT_SLAVE) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _slaveSelected = true;

        // Done
        return true;
    }

    public boolean hwusrtSelectSlave()
    { return _hwusrtSelectSlave_impl(true); }

    public boolean _hwusrtSelectSlaveNCS()
    { return _hwusrtSelectSlave_impl(false); }

    protected boolean _hwusrtDeselectSlave_impl(final boolean errorIfNotSelected)
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Error if not selected (optional)
        if(errorIfNotSelected && !_slaveSelected) return false;

        // Deselect the slave
        if( !_writeCmd(CMD_HW_USRT_DESELECT_SLAVE) ) return false;

        if( !_readAck() ) return false;

        // Clear flag
        _slaveSelected = false;

        // Done
        return true;
    }

    public boolean hwusrtDeselectSlave()
    { return _hwusrtDeselectSlave_impl(true); }

    public boolean _hwusrtDeselectSlaveNCS()
    { return _hwusrtDeselectSlave_impl(false); }

    public boolean hwusrtEnableTx()
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Enable Tx
        if( !_writeCmd(CMD_HW_USRT_ENABLE_TX) ) return false;

        return _readAck();
    }

    public boolean hwusrtDisableTx()
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Disable Tx
        if( !_writeCmd(CMD_HW_USRT_DISABLE_TX) ) return false;

        return _readAck();
    }

    public boolean hwusrtDisableTxAfter(final int nb)
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Ensure all the bytes are written
        while( _ttyPortS.bytesAwaitingWrite() > 0 );

        // Disable Tx after it finishes transmitting 'nb' bytes.
        if( !_writeCmd(CMD_HW_USRT_DISABLE_TX_AFTER, nb) ) return false;

        // Ensure all the bytes are written
        while( _ttyPortS.bytesAwaitingWrite() > 0 );

        /*
        if( !_readAck() ) return false;

        SysUtil.stdDbg().printf( ">>> hwusrtDisableTxAfter = %d <<< \n", _readRes(1)[0] );

        if(true) return true;
        //*/

        return _readAck();
    }

    public boolean hwusrtPulseXck(final int count, final boolean txValue)
    {
        // WARNING : Hardware USRT mode does not support manually pulsing the Xck line on request!
        return false;
    }

    public boolean hwusrtChangeBaudrate(final int baudrate)
    {
        // Use the function from HW-UART
        return uartChangeBaudrate(baudrate);
    }

    public boolean hwusrtTx(final int[] buff)
    {
        // Use the function from HW-UART
        return uartTx(buff);
    }

    public boolean hwusrtRx(final int[] buff)
    {
        // Use the function from HW-UART
        return uartRx(buff);
    }

    public boolean hwusrtTx_discardSerialLoopback(final int[] buff)
    {
        // Use the function from HW-UART
        return uartTx_discardSerialLoopback(buff);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean usrtBegin(final UXRTMode uxrtMode, final SSMode ssMode, final int baudrate)
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtBegin(uxrtMode, ssMode, baudrate, false);
        else                                   return bbusrtBegin(uxrtMode, ssMode, baudrate       );
    }

    @Override
    public boolean usrtBegin_PDI(final UXRTMode uxrtMode, final SSMode ssMode, final int baudrate)
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtBegin(uxrtMode, ssMode, baudrate, true );
        else                                   return false;
    }

    @Override
    public boolean usrtEnd()
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtEnd();
        else                                   return bbusrtEnd();
    }

    @Override
    public boolean usrtSelectSlave()
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtSelectSlave();
        else                                   return bbusrtSelectSlave();
    }

    @Override
    public boolean usrtDeselectSlave()
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtDeselectSlave();
        else                                   return bbusrtDeselectSlave();
    }

    @Override
    public boolean usrtEnableTx()
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtEnableTx();
        else                                   return bbusrtEnableTx();
    }

    @Override
    public boolean usrtDisableTx()
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtDisableTx();
        else                                   return bbusrtDisableTx();
    }

    @Override
    public boolean usrtDisableTxAfter(final int nb)
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtDisableTxAfter(nb);
        else                                   return bbusrtDisableTxAfter(nb);
    }

    @Override
    public boolean usrtPulseXck(final int count, final boolean txValue)
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtPulseXck(count, txValue);
        else                                   return bbusrtPulseXck(count, txValue);
    }

    @Override
    public boolean usrtChangeBaudrate(final int baudrate)
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtChangeBaudrate(baudrate);
        else                                   return bbusrtChangeBaudrate(baudrate);
    }

    @Override
    public boolean usrtTx(final int[] buff)
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtTx(buff);
        else                                   return bbusrtTx(buff);
    }

    @Override
    public boolean usrtRx(final int[] buff)
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtRx(buff);
        else                                   return bbusrtRx(buff);
    }

    @Override
    public boolean usrtTx_discardSerialLoopback(final int[] buff)
    {
        if(_usrtImplMode == ImplMode.Hardware) return hwusrtTx_discardSerialLoopback(buff);
        else                                   return bbusrtTx_discardSerialLoopback(buff);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean uartIsImplModeSupported(final ImplMode implMode)
    {
        // Only hardware mode is supported
        return implMode == ImplMode.Hardware;
    }

    @Override
    public boolean uartSetImplMode(final ImplMode implMode)
    {
        // Error if already initialized
        if(_initializedUXRT) return false;

        // Only hardware mode is supported
        return implMode == ImplMode.Hardware;
    }

    @Override
    public ImplMode uartGetImplMode()
    { return ImplMode.Hardware; }

    @Override
    public boolean uartIsOperationalModeSupported(final OperationalMode operationalMode)
    {
        // Only master mode is supported
        return operationalMode == OperationalMode.Master;
    }

    @Override
    public boolean uartIsDuplexModeSupported(final DuplexMode duplexMode)
    {
        // Both full duplex mode and half duplex mode are supported
        return true;
    }

    @Override
    public boolean uartIsEnablingDisablingTxSupported()
    {
        // The Hardware mode support manually enabling and disabling the Tx line on request
        return true;
    }

    @Override
    public int uartGetMinimumBaudrate()
    {
        // ( (F_CPU + 8 * baudrate) / (16 * baudrate) - 1 ) <= 4095
        final int min = F_CPU / 65528;
        for(int i = min - 1; i <= min + 1; ++i) {
            if( ( F_CPU / (16 * i) - 1 ) <= 4095 ) return i;
        }
        return UndeterminedFrequency;
    }

    @Override
    public int uartGetMaximumBaudrate()
    { return F_CPU / 8; }

    /*
     * NOTE : # Only hardware UART is supported.
     *        # These baudrates are supported by the current implementation with less than 3% error (other
     *          baudrates are also supported but the error may be greater than 3%).
     *
     * !!! NOTE : Always synchronize with the protocol manual and firmware implementation !!!
     */
    @Override
    public int[] uartGetStandardBaudrates()
    {
        /*
        final int[]                        bs = new int[] { uartGetMinimumBaudrate(), uartGetMaximumBaudrate(), 1, 245, 300, 600, 1200, 2400, 4800, 9600, 14400, 19200, 28800, 38400, 57600, 76800, 115200, 230400, 250000, 460800, 500000, 921600, 1000000, 2000000, 3000000, 4000000, 5000000, 6000000, 7000000, 8000000 };
        final java.util.ArrayList<Integer> us = new java.util.ArrayList<Integer>();
        for(final int b : bs) {
            if( b < uartGetMinimumBaudrate() || b > uartGetMaximumBaudrate() ) continue;
                  boolean u2x  = true;
                  int     ubbr = (F_CPU + 4 * b) / ( 8 * b) - 1;
            if(ubbr > 4095) {
                          u2x  = false;
                          ubbr = (F_CPU + 8 * b) / (16 * b) - 1;
            }
            if(ubbr < 0) break;
            final int     baud = F_CPU / (u2x ? 8 : 16) / (ubbr + 1);
            final float errr  = ( ( (float) baud / (float) b ) - 1.0f ) * 100.0f;
            if(ubbr <= 4095) {
                SysUtil.stdDbg().printf("%7d : %4d (%+5.1f)\n", b, ubbr, errr);
                if( Math.abs(errr) <= 3.0 ) us.add(b);
            }
        }
        SysUtil.stdDbg().println( us.toString() );
        //*/

        return new int[] { 300, 600, 1200, 2400, 4800, 9600, 14400, 19200, 28800, 38400, 57600, 76800, 115200, 250000, 500000, 1000000, 2000000 };
    }

    @Override
    public boolean uartBegin(final UXRTMode uxrtMode, final int baudrate)
    {
        // Error if already initialized
        if(_initializedUXRT) return false;

        // Apply the settings
        USB2GPIO.ttyPortApplyConfig(_ttyPortS, uxrtMode, baudrate);

        // Enable the HW-UART
        if( !hwuartEnable() ) {
            if( !hwuartDisable() ) return false;
            if( !hwuartEnable () ) return false;
        }

        // Set flag
        _initializedUXRT = true;

        // Done
        return true;
    }

    @Override
    public boolean uartEnd()
    {
        // Error if not initialized
        if(!_initializedUXRT) {
            // Force disable
            hwuartDisable();
            // Return error
            return false;
        }

        // Restore the default settings
        USB2GPIO.ttyPortApplyConfig(_ttyPortS, UXRTMode._8N1, DEFAULT_BAUDRATE_S);

        // Disable the HW-UART
        hwuartDisable();

        // Clear flag
        _initializedUXRT = false;

        // Done
        return true;
    }

    @Override
    public boolean uartEnableTx()
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Enable Tx
        if( !_writeCmd(CMD_HW_UART_ENABLE_TX) ) return false;

        return _readAck();
    }

    @Override
    public boolean uartDisableTx()
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Disable Tx
        if( !_writeCmd(CMD_HW_UART_DISABLE_TX) ) return false;

        return _readAck();
    }

    @Override
    public boolean uartDisableTxAfter(final int nb)
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Disable Tx after it finishes transmitting 'nb' bytes
        if( !_writeCmd(CMD_HW_UART_DISABLE_TX_AFTER, nb) ) return false;

        return _readAck();
    }

    @Override
    public boolean uartChangeBaudrate(final int baudrate)
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Restore the settings
        _ttyPortS.setBaudRate​(baudrate);

        // Done
        return true;
    }

    @Override
    public boolean uartTx(final int[] buff)
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Prepare the buffer
        final byte[] buf = USB2GPIO.ia2ba(buff);
              int    len = buf.length;
              int    ofs = 0;

        // Write the command byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS);

        while(len != 0) {
            // Write some byte(s)
            final int cnt = _ttyPortS.writeBytes(buf, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                                  }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_TxTimeout, DevClassNameHWUART); }
        }

        // Ensure all the bytes are written
        while( _ttyPortS.bytesAwaitingWrite() > 0 );

        // Done
        return true;
    }

    @Override
    public boolean uartRx(final int[] buff)
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Prepare the buffer
        final byte[] buf = new byte[buff.length];
              int    len = buf.length;
              int    ofs = 0;

        // Read the response byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS);

        while(len != 0) {
            // Read some byte(s)
            final int cnt = _ttyPortS.readBytes​(buf, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                                  }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, DevClassNameHWUART); }
        }

        // Convert to integer(s)
        USB2GPIO.ba2ia(buff, buf);

        // Done
        return true;
    }

    @Override
    public boolean uartTx_discardSerialLoopback(final int[] buff)
    {
        // Error if not initialized
        if(!_initializedUXRT) return false;

        // Prepare the buffer
        final byte[] buf = USB2GPIO.ia2ba(buff);
              int    len = buf.length;
              int    ofs = 0;

        // Clear all the buffers
        _ttyPortS.flushIOBuffers();

        // Write the command byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS);

        while(len != 0) {
            // Write some byte(s)
            final int cnt = _ttyPortS.writeBytes(buf, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                                  }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_TxTimeout, DevClassNameHWUART); }
        }

        // Ensure all the bytes are written
        while( _ttyPortS.bytesAwaitingWrite() > 0 );

        // Read the echo
        final int[] chkb = new int[buff.length];

        if( !uartRx(chkb) ) return false;

        /*
        if(true) {
            for(int i = 0; i < buff.length;++i) {
                SysUtil.stdDbg().printf("%02X -> %02X\n", buff[i], chkb[i]);
            }
            return true;
        }
        //*/

        // Done
        return Arrays.equals(chkb, buff);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean swimIsImplModeSupported(final ImplMode implMode)
    {
        // Only bit banging mode is supported
        return implMode == ImplMode.BitBang;
    }

    @Override
    public boolean swimSetImplMode(final ImplMode implMode)
    {
        // Error if already initialized
        if(_initializedSWIM) return false;

        // Only bit banging mode is supported
        return implMode == ImplMode.BitBang;
    }

    @Override
    public ImplMode swimGetImplMode()
    { return ImplMode.BitBang; }

    @Override
    public boolean swimBegin()
    {
        // Error if already initialized
        if(_initializedSWIM) return false;

        // Enable SWIM
        if( !_writeCmd(CMD_BB_SWIM_ENABLE) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _initializedSWIM = true;

        // Done
        return true;
    }

    @Override
    public boolean swimEnd()
    {
        // Error if not initialized
        if(!_initializedSWIM) {
            // Force disable
            if( _writeCmd(CMD_BB_SWIM_DISABLE) ) _readAck();
            // Return error
            return false;
        }

        // Disable SWIM
        if( !_writeCmd(CMD_BB_SWIM_DISABLE) ) return false;

        if( !_readAck() ) return false;

        // Clear flag
        _initializedSWIM = false;

        // Done
        return true;
    }

    @Override
    public boolean swimLineReset()
    {
        // Error if not initialized
        if(!_initializedSWIM) return false;

        // Select the slave
        if( !_writeCmd(CMD_BB_SWIM_LINE_RESET) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _slaveSelected = true;

        // Done
        return true;
    }

    @Override
    public boolean swimTransfer(final int[] ioBuff, final int len2X)
    {
        // Error if not initialized
        if(!_initializedSWIM) return false;

        // [!!! v1 Protocol ONLY !!!]
        if( !isHWv1() ) return false;

        // Error if the number of bits is too many
        if( ioBuff.length > BB_SWIM_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) return false;

        if(len2X >= ioBuff.length) return false;

        // Send the command and data
        if( !_writeCmdExt(ioBuff, CMD_BB_SWIM_TRANSFER, ioBuff.length, len2X) ) return false;

        if( !_readAck() ) return false;

        // Receive the response
        if( _readRes(ioBuff.length, ioBuff) == null ) return false;

        // Done
        return true;
    }

    @Override
    public boolean swimCmd_SRST()
    {
        // Error if not initialized
        if(!_initializedSWIM) return false;

        // Select the slave
        if( !_writeCmd(CMD_BB_SWIM_SRST) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _slaveSelected = true;

        // Done
        return true;
    }

    @Override
    public boolean swimCmd_ROTF(final int[] buff, final int address24)
    {
        // Error if not initialized
        if(!_initializedSWIM) return false;

        // Error if the number of bytes is too many
        if( buff.length > BB_SWIM_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) return false;

        // Send the command and data
        if( !_writeCmd( CMD_BB_SWIM_ROTF, buff.length, (address24 >> 16) & 0xFF, (address24 >> 8) & 0xFF, address24 & 0xFF ) ) return false;

        if( !_readAck() ) return false;

        // Receive the response
        if( _readRes(buff.length, buff) == null ) return false;

        // Done
        return true;
    }

    @Override
    public boolean swimCmd_WOTF(final int[] buff, final int address24)
    {
        // Error if not initialized
        if(!_initializedSWIM) return false;

        // Error if the number of bytes is too many
        if( buff.length > BB_SWIM_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) return false;

        // Send the command and data
        if( !_writeCmdExt( buff, CMD_BB_SWIM_WOTF, buff.length, (address24 >> 16) & 0xFF, (address24 >> 8) & 0xFF, address24 & 0xFF ) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : The v1 JxMake USB-GPIO hardware does not support JTAG

    // ##### !!! TODO : VERIFY !!! #####

    @Override
    public boolean jtagIsModeSupported()
    { return isHWv2(); }

    @Override
    public boolean jtagBegin(final int clkDiv)
    {
        // [!!! v2 Protocol ONLY !!!]
        if( !jtagIsModeSupported() ) return false;

        // Error if already initialized
        if(_initializedJTAG) return false;

        if( !_writeCmd(CMD_JTAG_ENABLE, clkDiv & 0x7F) ) return false;

        if( !_readAck() ) return false;

        // Set flag
        _initializedJTAG = true;

        // Done
        return true;
    }

    @Override
    public boolean jtagEnd()
    {
        // Error if not initialized
        if(!_initializedJTAG) {
            // Force disable
            if( _writeCmd(CMD_JTAG_DISABLE) ) _readAck();
            // Return error
            return false;
        }

        // Disable JTAG
        if( !_writeCmd(CMD_JTAG_DISABLE) ) return false;

        if( !_readAck() ) return false;

        // Clear flag
        _initializedJTAG = false;

        // Done
        return true;
    }

    @Override
    public boolean jtagSetClkDiv(final int clkDiv)
    {
        // Error if not initialized
        if(!_initializedJTAG) return false;

        // Set the JTAG clock frequency
        if( !_writeCmd(CMD_JTAG_SET_FREQUENCY, clkDiv & 0x7F) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    @Override
    public boolean jtagSetReset(final boolean nRST, final boolean nTRST, final boolean TDI)
    {
        // Error if not initialized
        if(!_initializedJTAG) return false;

        // Set the JTAG reset
        if( !_writeCmd( CMD_JTAG_SET_RESET, (nRST ? 0x80 : 0x00) | (nTRST ? 0x40 : 0x00) | (TDI ? 0x20 : 0x00) ) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    @Override
    public boolean jtagTMS(final boolean nRST, final boolean nTRST, final boolean TDI, final int bitNumMinusOne, final int value)
    {
        // Error if not initialized
        if(!_initializedJTAG) return false;

        // Send the TMS bits
        if( !_writeCmd( CMD_JTAG_TMS, (nRST ? 0x80 : 0x00) | (nTRST ? 0x40 : 0x00) | (TDI ? 0x20 : 0x00) | (bitNumMinusOne & 0x07), value ) ) return false;

        if( !_readAck() ) return false;

        // Done
        return true;
    }

    @Override
    public boolean jtagTransfer(boolean xUpdate, boolean drShift, boolean irShift, int bitCntLastMinusOne, final int[] ioBuff)
    {
        // Error if not initialized
        if(!_initializedJTAG) return false;

        // Error if the number of bytes is too many
        if( ioBuff.length > JTAG_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) return false;

        // Prepare the configuration bits
        final int cfb = (xUpdate ? 0x80 : 0x00) | (drShift ? 0x40 : 0x00)  | (irShift ? 0x20 : 0x00) | (bitCntLastMinusOne & 0x07);

        // Send the command and data
        if( !_writeCmdExt(ioBuff, CMD_JTAG_TRANSFER, cfb, ioBuff.length) ) return false;

        if( !_readAck() ) return false;

        // Receive the response
        if( _readRes(ioBuff.length, ioBuff) == null ) return false;

        // Done
        return true;
    }

    @Override
    public boolean jtagXBTransfer(boolean xUpdate, boolean drShift, boolean irShift, final int[] ioBuff)
    {
        // ##### ??? TODO : Remove CMD_JTAG_XB_TRANSFER because it does not seem to be needed ??? #####

        // Error if not initialized
        if(!_initializedJTAG) return false;

        // Error if the number of bytes is too many
        if( ioBuff.length > JTAG_TRANSFER_MAX_SIZE[ _getPvIdx() ] ) return false;

        // Error if the number of bytes is not even
        if( (ioBuff.length % 2) != 0 ) return false;

        // Prepare the configuration bits
        final int cfb = (xUpdate ? 0x80 : 0x00) | (drShift ? 0x40 : 0x00)  | (irShift ? 0x20 : 0x00);

        // Send the command and data
        if( !_writeCmdExt(ioBuff, CMD_JTAG_XB_TRANSFER, cfb, ioBuff.length / 2) ) return false;

        if( !_readAck() ) return false;

        // Receive the response
        if( _readRes(ioBuff.length, ioBuff) == null ) return false;

        // Done
        return true;
    }

} // class USB_GPIO
