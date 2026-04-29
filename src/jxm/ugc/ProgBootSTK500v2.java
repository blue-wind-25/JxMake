/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.Arrays;
import java.util.function.IntConsumer;

import jxm.*;
import jxm.xb.*;


/*
 * Minimal implementation of the STK500v2 protocol for programming MCUs with compatible bootloaders
 * (e.g., 'wiring').
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written partially based on the algorithms and information found from:
 *
 *     AVR068 - STK500 Communication Protocol
 *     https://ww1.microchip.com/downloads/en/AppNotes/doc2591.pdf
 *
 *     Arduino Serial Bootloader
 *     https://github.com/arduino/ArduinoCore-avr/tree/master/bootloaders/stk500v2
 *     GNU GPL Version 2 (or later) License
 *
 *     Wiring Bootloaders
 *     https://github.com/WiringProject/Wiring/tree/master/framework/hardware/Wiring/bootloaders
 *     GNU GPL Version 3 (or later) License
 *     https://github.com/WiringProject/Wiring/blob/master/framework/hardware/Wiring/bootloaders/License.txt
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * JxMake reimplements protocol behavior based on available documentation, supplemented by runtime
 * observation and bench-level validation where features are undocumented or ambiguous. No URBOOT
 * source code or expressive implementation logic is used. Constants and timing are derived from
 * functional analysis and do not constitute derivative work.
 */
public class ProgBootSTK500v2 extends ProgBootSerial {

    private static final String ProgClassName = "ProgBootSTK500v2";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class Config extends ProgBootSerial.Config {
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'xxx*()' functions ??? #####


public static Config ATtiny13()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 1024;
    config.memoryFlash.pageSize   =   32;
    config.memoryFlash.numPages   =   32;

    config.memoryEEPROM.totalSize =   64;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega8A()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

    return config;
}

public static Config ATmega16A()
{
    final Config config = ATmega8A();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;

    return config;
}

public static Config ATmega32A()
{
    final Config config = ATmega16A();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    return config;
}

public static Config ATmega64A()
{
    final Config config = ATmega32A();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;

    config.memoryEEPROM.totalSize =  2048;

    return config;
}

public static Config ATmega128A()
{
    final Config config = ATmega64A();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    config.memoryEEPROM.totalSize =   4096;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega48P()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 4096;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =   64;

    config.memoryEEPROM.totalSize =  256;

    return config;
}

public static Config ATmega88P()
{
    final Config config = ATmega48P();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

    return config;
}

public static Config ATmega168P()
{
    final Config config = ATmega88P();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;

    return config;
}

public static Config ATmega328P()
{
    final Config config = ATmega168P();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    return config;
}

public static Config ArduinoUnoR3     () { return ATmega328P(); }
public static Config ArduinoUnoRev3   () { return ATmega328P(); }
public static Config ArduinoNano      () { return ATmega328P(); }
public static Config ArduinoProMini168() { return ATmega168P(); }
public static Config ArduinoProMini328() { return ATmega328P(); }
public static Config ArduinoProMini   () { return ATmega328P(); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega640()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  4096;

    return config;
}

public static Config ATmega1280()
{
    final Config config = ATmega640();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    return config;
}

public static Config ATmega2560()
{
    final Config config = ATmega1280();

    config.memoryFlash.totalSize  = 262144;
    config.memoryFlash.numPages   =   1024;

    return config;
}

public static Config ArduinoMega1280() { return ATmega1280(); }
public static Config ArduinoMega2560() { return ATmega2560(); }
public static Config ArduinoMega    () { return ATmega2560(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // STK500v2 special constants
    private static final int MESSAGE_START                = 0x1B;
    private static final int TOKEN                        = 0x0E;

    private static final int ANSWER_CKSUM_ERROR           = 0xB0;

    // STK500v2 general commands
    private static final int CMD_SIGN_ON                  = 0x01;
    private static final int CMD_SET_PARAMETER            = 0x02;
    private static final int CMD_GET_PARAMETER            = 0x03;
    private static final int CMD_SET_DEVICE_PARAMETERS    = 0x04;
    private static final int CMD_OSCCAL                   = 0x05;
    private static final int CMD_LOAD_ADDRESS             = 0x06;
    private static final int CMD_FIRMWARE_UPGRADE         = 0x07;
    private static final int CMD_CHECK_TARGET_CONNECTION  = 0x0D;
    private static final int CMD_LOAD_RC_ID_TABLE         = 0x0E;
    private static final int CMD_LOAD_EC_ID_TABLE         = 0x0F;

    // STK500v2 ISP commands
    private static final int CMD_ENTER_PROGMODE_ISP       = 0x10;
    private static final int CMD_LEAVE_PROGMODE_ISP       = 0x11;
    private static final int CMD_CHIP_ERASE_ISP           = 0x12;
    private static final int CMD_PROGRAM_FLASH_ISP        = 0x13;
    private static final int CMD_READ_FLASH_ISP           = 0x14;
    private static final int CMD_PROGRAM_EEPROM_ISP       = 0x15;
    private static final int CMD_READ_EEPROM_ISP          = 0x16;
    private static final int CMD_PROGRAM_FUSE_ISP         = 0x17;
    private static final int CMD_READ_FUSE_ISP            = 0x18;
    private static final int CMD_PROGRAM_LOCK_ISP         = 0x19;
    private static final int CMD_READ_LOCK_ISP            = 0x1A;
    private static final int CMD_READ_SIGNATURE_ISP       = 0x1B;
    private static final int CMD_READ_OSCCAL_ISP          = 0x1C;
    private static final int CMD_SPI_MULTI                = 0x1D;

    // STK500v2 responses
    private static final int STATUS_CMD_OK                = 0x00;

    private static final int STATUS_CMD_TOUT              = 0x80;
    private static final int STATUS_RDY_BSY_TOUT          = 0x81;
    private static final int STATUS_SET_PARAM_MISSING     = 0x82;

    private static final int STATUS_CMD_FAILED            = 0xC0;
    private static final int STATUS_CKSUM_ERROR           = 0xC1;
    private static final int STATUS_CMD_UNKNOWN           = 0xC9;
    private static final int STATUS_CMD_ILLEGAL_PARAMETER = 0xCA;
    private static final int STATUS_PHY_ERROR             = 0xCB;
    private static final int STATUS_CLOCK_ERROR           = 0xCC;
    private static final int STATUS_BAUD_INVALID          = 0xCD;

    private static final int STATUS_ISP_READY             = 0x00;
    private static final int STATUS_CONN_FAIL_SDO         = 0x01;
    private static final int STATUS_CONN_FAIL_RST         = 0x02;
    private static final int STATUS_CONN_FAIL_SCK         = 0x04;
    private static final int STATUS_TGT_NOT_DETECTED      = 0x10;
    private static final int STATUS_TGT_REVERSE_INSERTED  = 0x20;

    // STK500v2 parameters
    private static final int PARAM_HW_VER                 = 0x90;
    private static final int PARAM_SW_MAJOR               = 0x91;
    private static final int PARAM_SW_MINOR               = 0x92;
    private static final int PARAM_VTARGET                = 0x94;
    private static final int PARAM_VADJUST                = 0x95;
    private static final int PARAM_OSC_PSCALE             = 0x96;
    private static final int PARAM_OSC_CMATCH             = 0x97;
    private static final int PARAM_SCK_DURATION           = 0x98;
    private static final int PARAM_TOPCARD_DETECT         = 0x9A;
    private static final int PARAM_STATUS                 = 0x9C;
    private static final int PARAM_DATA                   = 0x9D;
    private static final int PARAM_RESET_POLARITY         = 0x9E;
    private static final int PARAM_CONTROLLER_INIT        = 0x9F;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final int[] _msgBuff = new int[275];
    private int         _seqNumP = 0;
    private int         _seqNumC = 0; // 1

    private boolean _sendMsg(final int[] edata, final int... bytes)
    {
        final int msgSize = bytes.length + (edata != null ? edata.length : 0);
              int idx     = 0;
              int chksum  = 0;

         _seqNumP = (_seqNumC++) & 0xFF;
       //if(_seqNumC == 0) _seqNumC = 1;

        /* 0 */ _msgBuff[idx++] = MESSAGE_START;
        /* 1 */ _msgBuff[idx++] = _seqNumP;
        /* 2 */ _msgBuff[idx++] = (msgSize >> 8) & 0xFF;
        /* 3 */ _msgBuff[idx++] = (msgSize >> 0) & 0xFF;
        /* 4 */ _msgBuff[idx++] = TOKEN;

        for(int i = 0; i < idx; ++i) chksum ^= _msgBuff[i];

                            for(int i = 0; i < bytes.length; ++i) { _msgBuff[idx++] = bytes[i]; chksum ^= bytes[i]; }
        if(edata != null) { for(int i = 0; i < edata.length; ++i) { _msgBuff[idx++] = edata[i]; chksum ^= edata[i]; } }

        _msgBuff[idx++] = chksum;

        return _serialTx(_msgBuff, idx);
    }

    private boolean _sendMsg(final int... bytes)
    { return _sendMsg(null, bytes); }

    private int _recvMsg()
    {
        int chksum  = 0;

        if( !_serialRx(_pbs_rdBuff_32I, 5) ) { _serialDrain(); return -1; }

        if(_pbs_rdBuff_32I[0] != MESSAGE_START) { _serialDrain(); return -1; }
        if(_pbs_rdBuff_32I[1] != _seqNumP     ) { _serialDrain(); return -1; }
        if(_pbs_rdBuff_32I[4] != TOKEN        ) { _serialDrain(); return -1; }

        final int msgSize = (_pbs_rdBuff_32I[2] << 8) | _pbs_rdBuff_32I[3];

        Arrays.fill(_msgBuff, -1);

        if(msgSize > 0) {
            if( !_serialRx(_msgBuff, msgSize) ) { _serialDrain(); return -1; }
        }

        for(int i = 0; i < 5      ; ++i) chksum ^= _pbs_rdBuff_32I[i];
        for(int i = 0; i < msgSize; ++i) chksum ^= _msgBuff       [i];

        if( !_serialRx(_pbs_rdBuff_32I, 1) ) { _serialDrain(); return -1; }

        if(_pbs_rdBuff_32I[0] != chksum)  { _serialDrain(); return -1; }

        return msgSize;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int GET_SIGN_ON_RETRY_COUNT = 32;
    private static final int GET_SIGN_ON_RESET_SKIP  =  4;

    private String _getSignOn_impl()
    {
        // Send the command
        if( !_sendMsg(CMD_SIGN_ON) ) return null;

        // Get and check the response
        final int res = _recvMsg();
        if(res <= 0) return null;

        if(_msgBuff[0] != CMD_SIGN_ON  ) return null;
        if(_msgBuff[1] != STATUS_CMD_OK) return null;
        if(_msgBuff[2] != 8            ) return null;

        // Return the result
        return USB2GPIO.ia2str(_msgBuff, 3);
    }

    private String _getSignOn()
    {
        // Send the command as several times
        for(int i = 0; i < GET_SIGN_ON_RETRY_COUNT; ++i) {
            // Reset the MCU via DTR/RTS (only once every few attempts, just in case the bootloader requires a longer startup time)
            if( (i % GET_SIGN_ON_RESET_SKIP) == 0 ) {
                _serialResetMCU_DTR_RTS();
                SysUtil.sleepMS(25);
            }
            // Reset the sequnce number
            _seqNumC = 0;
            // Execute the command twice
            final String res1 = _getSignOn_impl();
            if(res1 != null) return res1;
            final String res2 = _getSignOn_impl();
            if(res2 != null) return res2;
        }

        // Not done
        return null;
    }

    private int _getParameter(final int paramType)
    {
        // Send the command
        if( !_sendMsg(CMD_GET_PARAMETER, paramType) ) return -1;

        // Get and check the response
        final int res = _recvMsg();
        if(res <= 0) return -1;

        if(_msgBuff[0] != CMD_GET_PARAMETER) return -1;
        if(_msgBuff[1] != STATUS_CMD_OK    ) return -1;

        // Return the result
        return _msgBuff[2];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _enterProgMode()
    {
        // Send the command
        if( !_sendMsg(CMD_ENTER_PROGMODE_ISP, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1) ) return false;

        // Get and check the response
        final int res = _recvMsg();
        if(res <= 0) return false;

        if(_msgBuff[0] != CMD_ENTER_PROGMODE_ISP) return false;
        if(_msgBuff[1] != STATUS_CMD_OK         ) return false;

        // Done
        return true;
    }

    private boolean _leaveProgMode()
    {
        // Send the command
        if( !_sendMsg(CMD_LEAVE_PROGMODE_ISP, -1, -1) ) return false;

        // Get and check the response
        final int res = _recvMsg();
        if(res <= 0) return false;

        if(_msgBuff[0] != CMD_LEAVE_PROGMODE_ISP) return false;
        if(_msgBuff[1] != STATUS_CMD_OK         ) return false;

        // Done
        return true;
    }

    private boolean _loadAddress(final int address, final boolean useWordAddress)
    {
        // Determine the division factor and calcualte the updated address
        final long    ADF  = useWordAddress ? 2 : 1;
        final boolean UXA  = _config.memoryFlash.totalSize > (64 * 1024);
        final long    addr = ( ( (long) address ) / ADF ) | (UXA ? 0x80000000L : 0L);

        // Send the command
        if( !_sendMsg (CMD_LOAD_ADDRESS, (int) ( (addr >> 24) & 0xFF ), (int) ( (addr >> 16) & 0xFF ), (int) ( (addr >> 8) & 0xFF ), (int) (addr & 0xFF) ) ) return false;

        // Get and check the response
        final int res = _recvMsg();
        if(res <= 0) return false;

        if(_msgBuff[0] != CMD_LOAD_ADDRESS) return false;
        if(_msgBuff[1] != STATUS_CMD_OK   ) return false;

        // Done
        return true;
    }

    private boolean _readFlashPage(final int[] buff, final int address)
    {
        // NOTE : Flash memory can be read without page-aligned address and/or size

        // Load the address first
        if( !_loadAddress(address, true) ) return false;

        // Send the command
        final int endAddr  = Math.min(address + _config.memoryFlash.pageSize, _config.memoryFlash.totalSize);
        final int readSize = endAddr - address;

        if( !_sendMsg ( CMD_READ_FLASH_ISP, (readSize >> 8) & 0xFF, readSize & 0xFF, -1 ) ) return false;

        // Get and check the response
        final int res = _recvMsg();
        if(res <= 0) return false;

        if(_msgBuff[0      ] != CMD_READ_FLASH_ISP) return false;
        if(_msgBuff[1      ] != STATUS_CMD_OK     ) return false;
        if(_msgBuff[res - 1] != STATUS_CMD_OK     ) return false;

        // Copy the bytes
        for(int i = 0; i < readSize; ++i) buff[i] = _msgBuff[i + 2];

        // Done
        return true;
    }

    private boolean _writeFlashPage(final int[] buff, final int address)
    {
        // Load the address first
        if( !_loadAddress(address, true) ) return false;

        // Send the command
        if( !_sendMsg ( buff, CMD_PROGRAM_FLASH_ISP, (buff.length >> 8) & 0xFF, buff.length & 0xFF, -1, -1, -1, -1, -1, -1, -1 ) ) return false;

        // Get and check the response
        final int res = _recvMsg();
        if(res <= 0) return false;

        if(_msgBuff[0] != CMD_PROGRAM_FLASH_ISP) return false;
        if(_msgBuff[1] != STATUS_CMD_OK        ) return false;

        // Done
        return true;
    }

    private int _readEEPROMByte(final int address)
    {
        // Load the address first
        if( !_loadAddress(address, false) ) return -1;

        // Send the command
        if( !_sendMsg ( CMD_READ_EEPROM_ISP, 0, 1, -1 ) ) return -1;

        // Get and check the response
        final int res = _recvMsg();
        if(res <= 0) return -1;

        if(_msgBuff[0      ] != CMD_READ_EEPROM_ISP) return -1;
        if(_msgBuff[1      ] != STATUS_CMD_OK      ) return -1;
        if(_msgBuff[res - 1] != STATUS_CMD_OK      ) return -1;

        // Done
        return _msgBuff[2];
    }

    private boolean _writeEEPROMByte(final int address, final int data)
    {
        // Load the address first
        if( !_loadAddress(address << 1, false) ) return false;

        // Send the command
        if( !_sendMsg ( CMD_PROGRAM_EEPROM_ISP, 0, 1, -1, -1, -1, -1, -1, -1, -1, data ) ) return false;

        // Get and check the response
        final int res = _recvMsg();
        if(res <= 0) return false;

        if(_msgBuff[0] != CMD_PROGRAM_EEPROM_ISP) return false;
        if(_msgBuff[1] != STATUS_CMD_OK         ) return false;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _inProgMode = false;
    private boolean _chipErased = false;

    public ProgBootSTK500v2(final ProgBootSTK500v2.Config config) throws Exception
    { super(ProgClassName, config); }

    @Override
    public byte _flashMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    @Override
    public int _flashMemoryAlignWriteSize(final int numBytes)
    { return USB2GPIO.alignWriteSize(numBytes, _config.memoryFlash.pageSize); }

    @Override
    public byte _eepromMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    private boolean _begin_impl(final String serialDevice, final int baudrate)
    {
        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Clear flag
        _chipErased = false;

        // Open the serial port
        if( !_openSerialPort(serialDevice, baudrate, null) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSerDev, ProgClassName);

        // Initialize the programmer
        if( _getSignOn    (              ) == null  ) return false;
        if( _getParameter (PARAM_HW_VER  ) <  0     ) return false;
        if( _getParameter (PARAM_SW_MAJOR) <  0     ) return false;
        if( _getParameter (PARAM_SW_MINOR) <  0     ) return false;
        if( _enterProgMode(              ) == false ) return false;

        // Set flag
        _inProgMode = true;

        // Done
        return true;
    }

    public boolean begin(final String serialDevice, final int baudrate)
    {
        if( _begin_impl(serialDevice, baudrate) ) return true;

        _closeSerialPort();

        return USB2GPIO.notifyError(Texts.ProgXXX_FailInitPrgDev, ProgClassName);
    }

    @Override
    public boolean end()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Leave programming mode
        final boolean resLeaveProgMode = _leaveProgMode();

        // Close the serial port
        _closeSerialPort();

        // Clear flag
        _inProgMode = false;

        // Check for error(s)
        if(!resLeaveProgMode) {
            if(!resLeaveProgMode) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPrgDev, ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean readSignature()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Read the bytes
        final int[] buff = new int[3];

        for(int i = 0; i < 3; ++i) {

            // Send the command
            if( !_sendMsg(CMD_READ_SIGNATURE_ISP, -1, -1, -1, i, -1) ) return false;

            // Get and check the response
            final int res = _recvMsg();
            if(res <= 0) return false;

            if(_msgBuff[0] != CMD_READ_SIGNATURE_ISP) return false;
            if(_msgBuff[1] != STATUS_CMD_OK         ) return false;
            if(_msgBuff[3] != STATUS_CMD_OK         ) return false;

            // Store the result
            buff[i] = _msgBuff[2];

        } // for

        // Store the signature bytes
        _mcuSignature = new int[] { buff[0], buff[1], buff[2] } ;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : This function is meant to erase flash and lock bits, however, it always returns error in (almost?) all
    //        bootloader implementations that use the STK500v2 protocol
    @Override
    public boolean chipErase()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_chipErased) return true;

        // ##### !!! TODO : Use '_writeFlashPage()' to erase the entire flash? !!! #####

        // Send the command
        _setSerialRxTimeout_MS_long(); // Set a much longer Rx timeout

        if( !_sendMsg(CMD_CHIP_ERASE_ISP, -1, -1, -1, -1, -1, -1) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        // Get and check the response
        final int res = _recvMsg();

        _setSerialRxTimeout_MS_default(); // Restore the default Rx timeout

        if(res <= 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
        if(_msgBuff[0] != CMD_CHIP_ERASE_ISP) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
        if(_msgBuff[1] != STATUS_CMD_FAILED ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        // Set flag
        _chipErased = true;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _verifyReadFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Determine the start address and number of bytes
        final int sa = (startAddress < 0) ? 0                             : startAddress;
        final int nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_even(sa, nb, _config.memoryFlash.totalSize, ProgClassName) ) return -1;

        // Prepare the result buffer
        if(_config.memoryFlash.readDataBuff == null || _config.memoryFlash.readDataBuff.length != numBytes) {
            _config.memoryFlash.readDataBuff = new int[numBytes];
        }

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Determine the number of chunks
        final int     ChunkSize  = _config.memoryFlash.pageSize;

        final boolean notAligned = (numBytes % ChunkSize) != 0;
        final int     numChunks  = (numBytes / ChunkSize) + (notAligned ? 1 : 0);

        // Read the bytes (and compare them if requested)
        final int[] cbytes = new int[ChunkSize];

              int   rdbIdx = 0;
              int   verIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, numBytes - rdbIdx);

            if( !_readFlashPage(cbytes, sa + c * ChunkSize) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                return -1;
            }

            // Process the chunk bytes
            for(int b = 0; b < numReads; b += 2) {

                // Store the bytes to the result buffer
                _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b    ];
                _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b + 1];

                // Compare the bytes as needed
                if(refData != null && verIdx < refData.length) {
                    if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                    ++verIdx;
                    if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                    ++verIdx;
                }

                // Call the progress callback function for the current value
                pcb.callProgressCallbackCurrent(progressCallback, nb);

            } // for b

        } // for c

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return numBytes;
    }

    @Override
    public boolean readFlash(final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(null, startAddress, numBytes, progressCallback) == numBytes; }

    private boolean _writeFlashPage_impl(final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_pageSize(sa, nb, _config.memoryFlash.pageSize, ProgClassName) ) return false;

        // Get the number of pages to be written and the current page address
        final int numPages = nb / _config.memoryFlash.pageSize;
              int cpgAddr  = sa;

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Clear flag
        _chipErased = false;

        // Write the pages
        int datIdx = 0;

        for(int p = 0; p < numPages; ++p) {

            // ##### !!! TODO : Skip writing the page if it is blank (its contents are all 'FlashMemory_EmptyValue') !!! #####

            // Write the page
            if( !_writeFlashPage( USB2GPIO.ba2ia(data, datIdx, _config.memoryFlash.pageSize), cpgAddr) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, _config.memoryFlash.pageSize / 2);

            // Increment the counters
            cpgAddr += _config.memoryFlash.pageSize;
            datIdx  += _config.memoryFlash.pageSize;

        } // for p

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return true;
    }

    @Override
    public boolean writeFlash(final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    {
        // Determine the start address and number of bytes
        final int sa = (startAddress < 0) ? 0                             : startAddress;
        final int nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        // Align the number of bytes and pad the buffer as needed
        final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer(data, sa, nb, _config.memoryFlash.pageSize, _config.memoryFlash.totalSize, FlashMemory_EmptyValue, ProgClassName);
        if(anbr == null) return false;

        // Write flash
        return _writeFlashPage_impl(anbr.buff, sa, anbr.nb, progressCallback);
    }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : Not all bootloader implementations that use the STK500v2 protocol support EEPROM reading and writing!

    @Override
    public int readEEPROM(final int address)
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Error if EEPROM is not available
        if(_config.memoryEEPROM.totalSize <= 0) {
            USB2GPIO.notifyError(Texts.ProgXXX_ENotAvailable, ProgClassName);
            return -1;
        }

        // Check the address
        if(address < 0 || address >= _config.memoryEEPROM.totalSize) {
            USB2GPIO.notifyError(Texts.ProgXXX_EAddrOoR, ProgClassName);
            return -1;
        }

        // Read and return the byte
        return _readEEPROMByte(address);
    }

    @Override
    public boolean writeEEPROM(final int address, final byte data)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Error if EEPROM is not available
        if(_config.memoryEEPROM.totalSize <= 0) return USB2GPIO.notifyError(Texts.ProgXXX_ENotAvailable, ProgClassName);

        // Check the address
        if(address < 0 || address >= _config.memoryEEPROM.totalSize) return USB2GPIO.notifyError(Texts.ProgXXX_EAddrOoR, ProgClassName);

        // Write the byte
        return _writeEEPROMByte(address, data);
    }

} // class ProgBootSTK500v2
