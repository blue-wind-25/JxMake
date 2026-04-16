/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.function.Function;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


/*
 * Base class for all 'ProgBoot*' subclasses interfacing with bootloaders over serial ports
 * (UART and CDC-ACM).
 */
public abstract class ProgBootSerial implements IProgCommon {

    /*
     * Transfer speed depends on the underlying protocol, baudrate, and the MCU's flash page size.
     *
     * Typically, speeds range from a few kilobytes per second to several tens of kilobytes per second
     * over a standard MCU UART port, and up to several hundred kilobytes per second over a native
     * MCU CDC (USB) port.
     */

    private   static final String DevClassName           = "ProgBootSerial";
    private   static       String ProgClassName;

    protected static final int    TX_RX_TIMEOUT_MS_DEFL  =  1000; // Default timeout
    protected static final int    TX_RX_TIMEOUT_MS_MIN   =   250; // Minimum timeout (e.g., for testing protocol command availability               )
    protected static final int    TX_RX_TIMEOUT_MS_SHORT =   500; // Short   timeout (e.g., when using the magic baudrate to trigger bootloader mode)
    protected static final int    TX_RX_TIMEOUT_MS_LONG  = 32000; // Long    timeout (e.g., chip erase or other extended operations                 )

    protected static final int    WAIT_DEV_TIMEOUT_MS    =  8000; // Timeout while waiting for the device to disappear and reappear

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : For most MCUs, the empty value of flash memory is 0xFF; however, on some MCUs, it may be 0x00
    protected static final byte FlashMemory_EmptyValue = (byte) 0xFF;

    @SuppressWarnings("serial")
    public static class Config extends SerializableDeepClone<Config> {

        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        // NOTE : The default values below are for (almost all?) AVR MCUs that can be programmed using ISP

        public static class MemoryFlash implements Serializable {
            public boolean paged        = true;
            public int     totalSize    = 0;
            public int     pageSize     = 0;
            public int     numPages     = 0;

            public int[]   readDataBuff = null;
        }

        public static class MemoryEEPROM implements Serializable {
            public int totalSize = 0;
            /*
            public boolean paged     = false;
            public int     totalSize = 0;
            public int     pageSize  = 0;
            public int     numPages  = 0;
            */
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final MemoryFlash  memoryFlash  = new MemoryFlash ();
        public final MemoryEEPROM memoryEEPROM = new MemoryEEPROM();

    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected Config _config;

    protected ProgBootSerial(final String progClassName, final Config config) throws Exception
    {
        // Store the programmer class name
        ProgClassName = progClassName;

        // Store the configuration
        _config = config.deepClone();

        // Check the configuration values
        if(_config.memoryFlash.totalSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFTotSize, ProgClassName);

        if(_config.memoryFlash.paged) {
            if(_config.memoryFlash.pageSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSize, ProgClassName);
            if(_config.memoryFlash.numPages <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFNumPages, ProgClassName);

            if(_config.memoryFlash.pageSize * _config.memoryFlash.numPages != _config.memoryFlash.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSpec, ProgClassName);
        }

        /*
        if(_config.memoryEEPROM.totalSize > 0 && _config.memoryEEPROM.paged) {
            if(_config.memoryEEPROM.pageSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEPageSize, ProgClassName);
            if(_config.memoryEEPROM.numPages <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMENumPages, ProgClassName);

            if(_config.memoryEEPROM.pageSize * _config.memoryEEPROM.numPages != _config.memoryEEPROM.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEPageSpec, ProgClassName);
        }
        */
    }

    public Config config()
    { return _config; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int _flashMemoryTotalSize()
    { return _config.memoryFlash.totalSize; }

    @Override
    public int _eepromMemoryTotalSize()
    { return _config.memoryEEPROM.totalSize; }

    @Override
    public int[] _readDataBuff()
    { return _config.memoryFlash.readDataBuff; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected int[] _mcuSignature = null;

    // NOTE : By default, assume the bootloader is able to read the device signature/ID
    @Override
    public boolean supportSignature()
    { return true; }

    @Override
    public boolean verifySignature(final int[] signatureBytes)
    {
        // Error if the signature has not been read
        if(_mcuSignature == null) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Compare the signature
        return Arrays.equals(_mcuSignature, signatureBytes);
    }

    @Override
    public int[] mcuSignature()
    {
        // Error if the signature has not been read
        if(_mcuSignature == null) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return null;
        }

        // Return the signature
        return _mcuSignature;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : Since bootloader programmers do not have full support for reading and writing lock bits,
    //           this feature is not implemented here!

    @Override
    public long readLockBits()
    { return -1; }

    @Override
    public boolean writeLockBits(final long value)
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected SerialDevice _ttyPort        = null;

    protected final byte[] _pbs_rdBuff_32B = new byte[32];
    protected final byte[] _pbs_wrBuff_32B = new byte[32];

    protected final int [] _pbs_rdBuff_32I = new int [32];
    protected final int [] _pbs_wrBuff_32I = new int [32];

    private void _openSerialPortXXX_impl(final String serialDevice, final int baudrate, final int parity, final boolean use_XON_XOFF)
    {
        // Get the device
        _ttyPort = SerialDevice.getCommPort(serialDevice);

        // Open the device
        final int flowControl = use_XON_XOFF // XON = 0x11 (DC1)   ;   XOFF = 0x13 (DC3)
                              ? (SerialDevice.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialDevice.FLOW_CONTROL_XONXOFF_OUT_ENABLED)
                              : (SerialDevice.FLOW_CONTROL_DISABLED                                                          );

        _ttyPort.setFlowControl​    (flowControl);
        _ttyPort.setBaudRate​       (baudrate);
        _ttyPort.setNumDataBits​    (8);
        _ttyPort.setParity​         (parity);
        _ttyPort.setNumStopBits​    (SerialDevice.ONE_STOP_BIT);
        _ttyPort.setComPortTimeouts​(SerialDevice.TIMEOUT_NONBLOCKING, 1000, 1000);
        _ttyPort.clearBreak        ();
        _ttyPort.clearDTR          ();
        _ttyPort.clearRTS          ();
        _ttyPort.openPort          ();
    }

    private void _openSerialPortNoParity_impl(final String serialDevice, final int baudrate, final boolean use_XON_XOFF)
    { _openSerialPortXXX_impl(serialDevice, baudrate, SerialDevice.NO_PARITY, use_XON_XOFF); }

    protected boolean _openSerialPort(final String serialDevice, final int baudrate, final Function<ProgBootSerial, Integer> checkAndGetMagicResetBLBaudrate, final boolean use_XON_XOFF)
    {
        // Open the device using the specified baudrate
        _openSerialPortNoParity_impl(serialDevice, baudrate, use_XON_XOFF);

        // Wait for a while
        SysUtil.sleepMS(300);

        // Check if the device may already be in bootloader mode; if it is not, the function will
        // return the magic baudrate to reset the module to bootloader mode
        int magicBaudrate = 0;

        if(checkAndGetMagicResetBLBaudrate != null) {
            _setSerialRxTimeout_MS_short();                              // Set a shorter Rx timeout
            USB2GPIO.redirectNotifyErrorToString(true);                  // Suppress all error message notifications.
            magicBaudrate = checkAndGetMagicResetBLBaudrate.apply(this); // Call the checker function
            USB2GPIO.redirectNotifyErrorToString(false);                 // Restore error message notification behavior
            _setSerialRxTimeout_MS_default();                            // Restore the default Rx timeout
        }

        // Open and close the device using the magic baudrate to reset it to bootloader mode
        if(magicBaudrate > 0) {

            // Change to the magic baudrate and toggle some control lines
            _ttyPort.setBaudRate(magicBaudrate);
            _ttyPort.setDTR     ();
            _ttyPort.setRTS     ();
            _ttyPort.clearDTR   ();
            _ttyPort.clearRTS   ();

            // Close the device
            _closeSerialPort();

            // Wait until the device disappear and then reappear
            if( !SerialDevice.waitForReEnumeration(serialDevice, WAIT_DEV_TIMEOUT_MS, 100) ) return USB2GPIO.notifyError(Texts.PDevXXX_DevTimeoutCSt, DevClassName);

        } // if

        // Open the device using the specified baudrate again if needed
        if(_ttyPort == null) _openSerialPortNoParity_impl(serialDevice, baudrate, use_XON_XOFF);

        // Done
        return true;
    }

    protected boolean _openSerialPort(final String serialDevice, final int baudrate, final Function<ProgBootSerial, Integer> checkAndGetMagicResetBLBaudrate)
    { return _openSerialPort(serialDevice, baudrate, checkAndGetMagicResetBLBaudrate, false); }

    protected void _openSerialPortEvenParity(final String serialDevice, final int baudrate)
    { _openSerialPortXXX_impl(serialDevice, baudrate, SerialDevice.EVEN_PARITY, false); }

    protected void _openSerialPortOddParity(final String serialDevice, final int baudrate)
    { _openSerialPortXXX_impl(serialDevice, baudrate, SerialDevice.ODD_PARITY, false); }

    protected void _closeSerialPort()
    {
        if(_ttyPort == null) return;

        _ttyPort.closePort();

        _ttyPort = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void _serialFlush()
    { _ttyPort.flushIOBuffers(); }

    protected void _serialDrain()
    { _ttyPort.serialDrain(); }

    protected void _serialResetMCU_DTR_RTS()
    { _ttyPort.pulse_DTR_RTS(100, 100); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean _serialTx(final byte[] buff, final int size)
    {
        // Prepare the buffer
        int len = size;
        int ofs = 0;

        // Write the byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS_DEFL);

        while(len != 0) {
            // Write some byte(s)
            final int cnt = _ttyPort.writeBytes(buff, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                            }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_TxTimeout, DevClassName); }
        }

        // Ensure all the bytes are written
        while( _ttyPort.bytesAwaitingWrite() > 0 );

        // Done
        return true;
    }

    protected boolean _serialTx(final byte[] buff)
    { return _serialTx(buff, buff.length); }

    protected boolean _serialTx(final int[] buff, final int size)
    { return _serialTx( USB2GPIO.ia2ba(buff), size ); }

    protected boolean _serialTx(final int[] buff)
    { return _serialTx(buff, buff.length); }

    protected boolean _serialTx(final char data)
    {
        _pbs_wrBuff_32B[0] = (byte) (data & 0xFF);

        return _serialTx(_pbs_wrBuff_32B, 1);
    }

    protected boolean _serialTx(final int data)
    {
        _pbs_wrBuff_32B[0] = (byte) (data & 0xFF);

        return _serialTx(_pbs_wrBuff_32B, 1);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int _CUR_RX_TIMEOUT_MS = TX_RX_TIMEOUT_MS_DEFL;

    protected int _getSerialRxTimeout_MS()
    { return _CUR_RX_TIMEOUT_MS; }

    protected void _setSerialRxTimeout_MS(final int timeout_MS)
    {
        _CUR_RX_TIMEOUT_MS = timeout_MS;
        if(_CUR_RX_TIMEOUT_MS <= 0) _CUR_RX_TIMEOUT_MS = TX_RX_TIMEOUT_MS_DEFL;
    }

    protected void _setSerialRxTimeout_MS_default() { _setSerialRxTimeout_MS(TX_RX_TIMEOUT_MS_DEFL ); }
    protected void _setSerialRxTimeout_MS_minimum() { _setSerialRxTimeout_MS(TX_RX_TIMEOUT_MS_MIN  ); }
    protected void _setSerialRxTimeout_MS_short  () { _setSerialRxTimeout_MS(TX_RX_TIMEOUT_MS_SHORT); }
    protected void _setSerialRxTimeout_MS_long   () { _setSerialRxTimeout_MS(TX_RX_TIMEOUT_MS_LONG ); }

    protected int serialRxUnreadCount()
    { return _ttyPort.bytesAvailable(); }

    protected boolean _serialRx(final byte[] buff, final int size)
    {
        // Prepare the buffer
        int len = size;
        int ofs = 0;

        // Read the byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(_CUR_RX_TIMEOUT_MS);

        while(len != 0) {
            // Read some byte(s)
            final int cnt = _ttyPort.readBytes​(buff, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                            }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, DevClassName); }
        }

        // Done
        return true;
    }

    protected boolean _serialRx(final byte[] buff)
    { return _serialRx(buff, buff.length); }

    protected boolean _serialRx(final int[] buff, final int size)
    {
        // Prepare the buffer
        final byte[] tmp = new byte[size];

        // Read the byte(s)
        if( !_serialRx(tmp) ) return false;

        // Convert to integer(s)
        USB2GPIO.ba2ia(buff, tmp);

        // Done
        return true;
    }

    protected boolean _serialRx(final int[] buff)
    { return _serialRx(buff, buff.length); }

    protected int[] _serialRx(final int size)
    {
        final int[] buff = new int[size];

        if( !_serialRx(buff, size) ) return null;

        return buff;
    }

    protected Integer _serialRxUInt8()
    {
        if( !_serialRx(_pbs_rdBuff_32B, 1) ) return null;

        return Integer.valueOf(_pbs_rdBuff_32B[0] & 0xFF);
    }

    protected Integer _serialRxUInt16LE()
    {
        if( !_serialRx(_pbs_rdBuff_32B, 2) ) return null;

        return Integer.valueOf( ( (_pbs_rdBuff_32B[1] & 0xFF) << 8 ) | (_pbs_rdBuff_32B[0] & 0xFF) );
    }

    protected Integer _serialRxUInt16BE()
    {
        if( !_serialRx(_pbs_rdBuff_32B, 2) ) return null;

        return Integer.valueOf( ( (_pbs_rdBuff_32B[0] & 0xFF) << 8 ) | (_pbs_rdBuff_32B[1] & 0xFF) );
    }

    protected Long _serialRxUInt32LE()
    {
        if( !_serialRx(_pbs_rdBuff_32B, 4) ) return null;

        return Long.valueOf( ( (_pbs_rdBuff_32B[3] & 0xFF) << 24 ) | ( (_pbs_rdBuff_32B[2] & 0xFF) << 16 ) | ( (_pbs_rdBuff_32B[1] & 0xFF) << 8 ) | (_pbs_rdBuff_32B[0] & 0xFF) );
    }

    protected Long _serialRxUInt32BE()
    {
        if( !_serialRx(_pbs_rdBuff_32B, 4) ) return null;

        return Long.valueOf( ( (_pbs_rdBuff_32B[0] & 0xFF) << 24 ) | ( (_pbs_rdBuff_32B[1] & 0xFF) << 16 ) | ( (_pbs_rdBuff_32B[2] & 0xFF) << 8 ) | (_pbs_rdBuff_32B[3] & 0xFF) );
    }

    protected boolean _serialRxChkUInt8(final int chkVal)
    {
        final Integer res = _serialRxUInt8();

        return (res != null) && (res == chkVal);
    }

    protected boolean _serialRxChkCR()
    { return _serialRxChkUInt8('\r'); }

    protected Character _serialRxChar()
    {
        if( !_serialRx(_pbs_rdBuff_32B, 1) ) return null;

        return Character.valueOf( (char) (_pbs_rdBuff_32B[0] & 0xFF) );
    }

    protected String _serialRxStr(final int len)
    {
        final byte[] buff = new byte[len];

        if( !_serialRx(buff) ) return null;

        try {
          //return new String(buff, "US-ASCII");
            return new String(buff, StandardCharsets.US_ASCII);
        }
        catch(final Exception e) {
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean _serialRxUntil(final byte[] buff, final int size, final byte[] terminator)
    {
        // Check if the terminator is not specified
        if(terminator == null) return _serialRx(buff, size);

        // Prepare the buffer
              int    ofs     = 0;
              int    termPos = 0;
        final byte[] tmp     = new byte[1];

        Arrays.fill( buff, (byte) 0 );

        // Read the byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(_CUR_RX_TIMEOUT_MS);

        while(size == 0 || ofs < size) {

            // Read one byte
            final int cnt = _ttyPort.readBytes(tmp, 1, 0);

            if(cnt < 0) return false; // Check for error

            // Process the byte and check for timeout
            if(cnt > 0) {

                // Match the terminator sequence
                if( tmp[0] == terminator[termPos] ) {
                    ++termPos;
                    if(termPos == terminator.length) return true;
                }
                // The terminator sequence does not match
                else {
                    // Store partially reeived terminator bytes as normal bytes
                    if(termPos > 0) {
                        for(int i = 0; i < termPos; ++i) {
                            if(ofs >= size) return USB2GPIO.notifyError(Texts.PDevXXX_RxOverflow, DevClassName);
                            buff[ofs++] = terminator[i];
                        }
                        termPos = 0;
                    }
                    // Store the current bytes
                    if(ofs >= size) return USB2GPIO.notifyError(Texts.PDevXXX_RxOverflow, DevClassName);
                    buff[ofs++] = tmp[0];
                }
                // Reset the timeout counter
                tms.reset();
            }
            else {
                if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, DevClassName);
            }

        } // while

        // Error because terminator is not found
        return false;
    }

    protected boolean _serialRxUntil(final byte[] buff, final byte[] terminator)
    { return _serialRxUntil(buff, buff.length, terminator); }

    protected boolean _serialRxUntil(final int[] buff, final int size, final byte[] terminator)
    {
        // Prepare the buffer
        final byte[] tmp = new byte[size];

        // Read the byte(s)
        if( !_serialRxUntil(tmp, terminator) ) return false;

        // Convert to integer(s)
        USB2GPIO.ba2ia(buff, tmp);

        // Done
        return true;
    }

    protected boolean _serialRxUntil(final int[] buff, final byte[] terminator)
    { return _serialRxUntil(buff, buff.length, terminator); }

    protected int[] _serialRxUntil(final int size, final byte[] terminator)
    {
        final int[] buff = new int[size];

        if( !_serialRxUntil(buff, size, terminator) ) return null;

        return buff;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean _serialRxUntil(final byte[] buff, final int size, final byte[] terminator1, final byte[] terminator2)
    {
        // Check if the terminator(s) are not specified
        if(terminator1 == null && terminator2 == null) return _serialRx     (buff, size             );
        if(terminator1 == null                       ) return _serialRxUntil(buff, size, terminator2);
        if(                       terminator2 == null) return _serialRxUntil(buff, size, terminator1);

        // Prepare the buffer
              int    ofs      = 0;
              int    termPos1 = 0;
              int    termPos2 = 0;
        final byte[] tmp      = new byte[1];

        Arrays.fill( buff, (byte) 0 );

        // Buffer to hold possible terminator fragments
        final ByteArrayOutputStream fragments = new ByteArrayOutputStream();

        // Read the byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(_CUR_RX_TIMEOUT_MS);

        while(size == 0 || ofs < size) {

            // Read one byte
            final int cnt = _ttyPort.readBytes(tmp, 1, 0);
            if(cnt < 0) return false; // Check for error

            // Process the byte and check for timeout
            if(cnt > 0) {

                //
                fragments.write(tmp[0]);

                // Match with the 1st terminator sequence
                if( tmp[0] == terminator1[termPos1] ) {
                    ++termPos1;
                    if(termPos1 == terminator1.length) return true;
                }
                else {
                    termPos1 = 0;
                }

                // Match with the 2nd terminator sequence
                if( tmp[0] == terminator2[termPos2] ) {
                    ++termPos2;
                    if(termPos2 == terminator2.length) return true;
                }
                else {
                    termPos2 = 0;
                }

                // If both matches failed to progress, flush and reset
                if(termPos1 == 0 && termPos2 == 0) {
                    for( final byte b : fragments.toByteArray() ) {
                        if(ofs >= size) return USB2GPIO.notifyError(Texts.PDevXXX_RxOverflow, DevClassName);
                        buff[ofs++] = b;
                    }
                    fragments.reset();
                }

                // Reset the timeout counter
                tms.reset();
            }
            else {
                if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, DevClassName);
            }

        } // while

        // Error because terminator is not found
        return false;
    }

    protected boolean _serialRxUntil(final byte[] buff, final byte[] terminator1, final byte[] terminator2)
    { return _serialRxUntil(buff, buff.length, terminator1, terminator2 ); }

    protected boolean _serialRxUntil(final int[] buff, final int size, final byte[] terminator1, final byte[] terminator2)
    {
        // Prepare the buffer
        final byte[] tmp = new byte[size];

        // Read the byte(s)
        if( !_serialRxUntil(tmp, terminator1, terminator2) ) return false;

        // Convert to integer(s)
        USB2GPIO.ba2ia(buff, tmp);

        // Done
        return true;
    }

    protected boolean _serialRxUntil(final int[] buff, final byte[] terminator1, final byte[] terminator2)
    { return _serialRxUntil(buff, buff.length, terminator1, terminator2); }

    protected int[] _serialRxUntil(final int size, final byte[] terminator1, final byte[] terminator2)
    {
        final int[] buff = new int[size];

        if( !_serialRxUntil(buff, size, terminator1, terminator2) ) return null;

        return buff;
    }

} // class ProgBootSerial
