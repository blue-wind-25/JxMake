/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.function.IntConsumer;

import jxm.*;
import jxm.xb.*;


/*
 * Minimal implementation of the STK500 protocol for programming MCUs with compatible bootloaders
 * (e.g., 'arduino').
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written partially based on the algorithms and information found from:
 *
 *     AVR061 - STK500 Communication Protocol
 *     https://www.microchip.com/content/dam/mchp/documents/OTH/ApplicationNotes/ApplicationNotes/doc2525.pdf
 *
 *     Arduino Serial Bootloader
 *     https://github.com/arduino/ArduinoCore-avr/tree/master/bootloaders/atmega
 *     https://github.com/arduino/ArduinoCore-avr/tree/master/bootloaders/atmega8
 *     https://github.com/arduino/ArduinoCore-avr/tree/master/bootloaders/bt
 *     https://github.com/arduino/ArduinoCore-avr/tree/master/bootloaders/lilypad
 *     https://github.com/arduino/ArduinoCore-avr/tree/master/bootloaders/optiboot
 *
 *     Optiboot Bootloader for Arduino and Atmel AVR
 *     https://github.com/Optiboot/optiboot
 *     Copyright (C) 2013-2021 by Bill Westfield
 *     Copyright (C) 2010      by Peter Knight
 *     GNU GPL Version 2 (or later) License with Bootloader Exception
 *     https://github.com/Optiboot/optiboot/blob/master/LICENSE
 *
 *     Optiboot Bootloader for DxCore
 *     https://github.com/SpenceKonde/DxCore/tree/master
 *     https://github.com/SpenceKonde/DxCore/tree/master/megaavr/bootloaders/optiboot_dx
 *     Copyright (C) 2020-2021 by Spence Konde
 *     Copyright (C) 2013-2019 by Bill Westfield
 *     Copyright (C) 2010      by Peter Knight
 *     GNU GPL Version 2 (or later) License
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * JxMake reimplements protocol behavior based on available documentation, supplemented by runtime
 * observation and bench-level validation where features are undocumented or ambiguous. No URBOOT
 * source code or expressive implementation logic is used. Constants and timing are derived from
 * functional analysis and do not constitute derivative work.
 */
public class ProgBootSTK500 extends ProgBootSerial {

    private static final String ProgClassName = "ProgBootSTK500";

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

    // STK500 responses
    private static final int RES_STK_OK                = 0x10; // DLE - data link escape
    private static final int RES_STK_FAILED            = 0x11; // DC1 - device control 1
    private static final int RES_STK_UNKNOWN           = 0x12; // DC2 - device control 2
    private static final int RES_STK_NODEVICE          = 0x13; // DC3 - device control 3
    private static final int RES_STK_INSYNC            = 0x14; // DC4 - device control 4
    private static final int RES_STK_NOSYNC            = 0x15; // NAK - negative acknowledge

    private static final int RES_ADC_CHANNEL_ERROR     = 0x16; // SYN - synchronous idle
    private static final int RES_ADC_MEASURE_OK        = 0x17; // ETB - end of transaction block
    private static final int RES_PWM_CHANNEL_ERROR     = 0x18; // CAN - cancel
    private static final int RES_PWM_ADJUST_OK         = 0x19; // EM  - end of medium

    // STK500 commands
    private static final int SYN_CRC_EOP               = 0x20; // ' '

    private static final int CMD_STK_GET_SYNC          = 0x30; // '0'
    private static final int CMD_STK_GET_SIGN_ON       = 0x31; // '1'

    private static final int CMD_STK_SET_PARAMETER     = 0x40; // '@'
    private static final int CMD_STK_GET_PARAMETER     = 0x41; // 'A'
    private static final int CMD_STK_SET_DEVICE        = 0x42; // 'B'
    private static final int CMD_STK_SET_DEVICE_EXT    = 0x45; // 'E'

    private static final int CMD_STK_ENTER_PROGMODE    = 0x50; // 'P'
    private static final int CMD_STK_LEAVE_PROGMODE    = 0x51; // 'Q'
    private static final int CMD_STK_CHIP_ERASE        = 0x52; // 'R'
    private static final int CMD_STK_CHECK_AUTOINC     = 0x53; // 'S'
    private static final int CMD_STK_LOAD_ADDRESS      = 0x55; // 'U'
    private static final int CMD_STK_UNIVERSAL         = 0x56; // 'V'
    private static final int CMD_STK_UNIVERSAL_MULTI   = 0x57; // 'W'

    private static final int CMD_STK_PROG_FLASH        = 0x60; // '`'
    private static final int CMD_STK_PROG_DATA         = 0x61; // 'a'
    private static final int CMD_STK_PROG_FUSE         = 0x62; // 'b'
    private static final int CMD_STK_PROG_LOCK         = 0x63; // 'c'
    private static final int CMD_STK_PROG_PAGE         = 0x64; // 'd'
    private static final int CMD_STK_PROG_FUSE_EXT     = 0x65; // 'e'

    private static final int CMD_STK_READ_FLASH        = 0x70; // 'p'
    private static final int CMD_STK_READ_DATA         = 0x71; // 'q'
    private static final int CMD_STK_READ_FUSE         = 0x72; // 'r'
    private static final int CMD_STK_READ_LOCK         = 0x73; // 's'
    private static final int CMD_STK_READ_PAGE         = 0x74; // 't'
    private static final int CMD_STK_READ_SIGN         = 0x75; // 'u'
    private static final int CMD_STK_READ_OSCCAL       = 0x76; // 'v'
    private static final int CMD_STK_READ_FUSE_EXT     = 0x77; // 'w'
    private static final int CMD_STK_READ_OSCCAL_EXT   = 0x78; // 'x'

    // STK500 parameters
    private static final int PAR_STK_HW_VER            = 0x80; // R
    private static final int PAR_STK_SW_MAJOR          = 0x81; // R
    private static final int PAR_STK_SW_MINOR          = 0x82; // R
    private static final int PAR_STK_LEDS              = 0x83; // R/W
    private static final int PAR_STK_VTARGET           = 0x84; // R/W
    private static final int PAR_STK_VADJUST           = 0x85; // R/W
    private static final int PAR_STK_OSC_PSCALE        = 0x86; // R/W
    private static final int PAR_STK_OSC_CMATCH        = 0x87; // R/W
    private static final int PAR_STK_RESET_DURATION    = 0x88; // R/W
    private static final int PAR_STK_SCK_DURATION      = 0x89; // R/W

    private static final int PAR_STK_BUFSIZEL          = 0x90; // R/W (range 0 - 255)
    private static final int PAR_STK_BUFSIZEH          = 0x91; // R/W (range 0 - 255)
    private static final int PAR_STK_DEVICE            = 0x92; // R/W (range 0 - 255)
    private static final int PAR_STK_PROGMODE          = 0x93; // 'P' or 'S'
    private static final int PAR_STK_PARAMODE          = 0x94; // 0 or 1
    private static final int PAR_STK_POLLING           = 0x95; // 0 or 1
    private static final int PAR_STK_SELFTIMED         = 0x96; // 0 or 1
    private static final int PAR_STK500_TOPCARD_DETECT = 0x98; // Detect the attached top-card

    // STK500 universal command operations
    private static final int AVR_OP_LOAD_EXT_ADDR      = 0x4D;
    private static final int AVR_OP_ERASE_FLASH        = 0xAC;

    // STK500 status bits
    private static final int BIT_STK_INSYNC            = 0x01; // INSYNC      status bit ; 1 = INSYNC
    private static final int BIT_STK_PROGMODE          = 0x02; // Programming mode       ; 1 = in programming mode
    private static final int BIT_STK_STANDALONE        = 0x04; // Standalone  mode       ; 1 = in standalone  mode
    private static final int BIT_STK_RESET             = 0x08; // Reset       button     ; 1 = button is pressed
    private static final int BIT_STK_PROGRAM           = 0x10; // Program     button     ; 1 = button is pressed
    private static final int BIT_STK_LEDG              = 0x20; // Green LED   status     ; 1 = LED is lit
    private static final int BIT_STK_LEDR              = 0x40; // Red LED     status     ; 1 = LED is lit
    private static final int BIT_STK_LEDBLINK          = 0x80; // LED blink   status     ; 1 = LED is blinking

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int GET_SYNC_RETRY_COUNT = 32;
    private static final int GET_SYNC_RESET_SKIP  =  4;

    private boolean _rxChk_INSYNC()
    { return _serialRxChkUInt8(RES_STK_INSYNC); }

    private boolean _rxChk_OK()
    { return _serialRxChkUInt8(RES_STK_OK); }

    private boolean _getSync()
    {
        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_GET_SYNC;
        _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

        // Send the command as several times
        for(int i = 0; i < GET_SYNC_RETRY_COUNT; ++i) {
            // Reset the MCU via DTR/RTS (only once every few attempts, just in case the bootloader requires a longer startup time)
            if( (i % GET_SYNC_RESET_SKIP) == 0 ) {
                _serialResetMCU_DTR_RTS();
                SysUtil.sleepMS(25);
            }
            // Send the command and check the response
            _serialTx(_pbs_wrBuff_32I, 2);
            if( _rxChk_INSYNC() ) return _rxChk_OK();
        }

        // Not done
        return false;
    }

    private String _getSignOn()
    {
        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_GET_SIGN_ON;
        _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

        // Send the command and check the response
        _serialTx(_pbs_wrBuff_32I, 2);

        if( !_rxChk_INSYNC() ) return null;

        final Integer res = _serialRxUInt8();
        if(res == null) return null;

        if(res == RES_STK_OK) return ""; // Some bootloaders do not return the text string

        final String msg = _serialRxStr(6);
        if(msg == null) return null;

        if( !_rxChk_OK() ) return null;

        // Done
        return ( (char) res.intValue() ) + msg;
    }

    private int _getParameter(final int paramType)
    {
        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_GET_PARAMETER;
        _pbs_wrBuff_32I[1] = paramType;
        _pbs_wrBuff_32I[2] = SYN_CRC_EOP;

        // Send the command and check the response
        _serialTx(_pbs_wrBuff_32I, 3);

        if( !_rxChk_INSYNC() ) return -1;

        final Integer res = _serialRxUInt8();
        if(res == null) return -1;

        if( !_rxChk_OK() ) return -1;

        // Done
        return res;
    }

    private boolean _stkUniversal(final int byte1, final int byte2, final int byte3, final int byte4)
    {
        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_UNIVERSAL;
        _pbs_wrBuff_32I[1] = byte1;
        _pbs_wrBuff_32I[2] = byte2;
        _pbs_wrBuff_32I[3] = byte3;
        _pbs_wrBuff_32I[4] = byte4;
        _pbs_wrBuff_32I[5] = SYN_CRC_EOP;

        // Send the command and check the response
        _serialTx(_pbs_wrBuff_32I, 6);

        if( !_rxChk_INSYNC() ) return false;

        final Integer res = _serialRxUInt8();
        if(res == null) return false;

        if( !_rxChk_OK() ) return false;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _enterProgMode()
    {
        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_ENTER_PROGMODE;
        _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

        // Send the command and check the response
        _serialTx(_pbs_wrBuff_32I, 2);

        if( !_rxChk_INSYNC() ) return false;

        // Done
        return _rxChk_OK();
    }

    private boolean _leaveProgMode()
    {
        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_LEAVE_PROGMODE;
        _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

        // Send the command and check the response
        _serialTx(_pbs_wrBuff_32I, 2);

        if( !_rxChk_INSYNC() ) return false;

        // Done
        return _rxChk_OK();
    }

    private boolean _loadAddress(final int address, final boolean useWordAddress)
    {
        // Determine the division factor and calcualte the updated address
        final int ADF  = useWordAddress ? 2 : 1;
        final int addr = address / ADF;

        // Send extended address if required
        if( (_config.memoryFlash.totalSize / ADF) > (64 * 1024) ) {
            if( !_stkUniversal(AVR_OP_LOAD_EXT_ADDR, 0x00, (addr >> 16) & 0xFF, 0x00) ) return false;
        }

        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_LOAD_ADDRESS;
        _pbs_wrBuff_32I[1] = (addr >> 0) & 0xFF;
        _pbs_wrBuff_32I[2] = (addr >> 8) & 0xFF;
        _pbs_wrBuff_32I[3] = SYN_CRC_EOP;

        // Send the command and check the response
        _serialTx(_pbs_wrBuff_32I, 4);

        if( !_rxChk_INSYNC() ) return false;

        // Done
        return _rxChk_OK();
    }

    private boolean _readPage(final int[] buff, final int address, final int pageSize, final char memType)
    {
        // Load the address first
        final boolean useWordAddress = (memType == 'F');

        if( !_loadAddress(address, useWordAddress) ) return false;

        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_READ_PAGE;
        _pbs_wrBuff_32I[1] = (pageSize >> 8) & 0xFF;
        _pbs_wrBuff_32I[2] = (pageSize >> 0) & 0xFF;
        _pbs_wrBuff_32I[3] = memType;
        _pbs_wrBuff_32I[4] = SYN_CRC_EOP;

        // Send the command and check the response
        _serialTx(_pbs_wrBuff_32I, 5);

        if( !_rxChk_INSYNC() ) return false;

        // Read the bytes
        if( !_serialRx(buff, pageSize) ) return false;

        // Done
        return _rxChk_OK();
    }

    private boolean _writePage(final int[] buff, final int address, final int pageSize, final char memType)
    {
        // Load the address first
        final boolean useWordAddress = (memType == 'F');

        if( !_loadAddress(address, useWordAddress) ) return false;

        // Prepare and send the command
        _pbs_wrBuff_32I[0] = CMD_STK_PROG_PAGE;
        _pbs_wrBuff_32I[1] = (pageSize >> 8) & 0xFF;
        _pbs_wrBuff_32I[2] = (pageSize >> 0) & 0xFF;
        _pbs_wrBuff_32I[3] = memType;

        _serialTx(_pbs_wrBuff_32I, 4);

        // Send the data
        _serialTx(buff);

        // Send the SYN_CRC_EOP
        _pbs_wrBuff_32I[0] = SYN_CRC_EOP;

        _serialTx(_pbs_wrBuff_32I, 1);

        // Check the response
        if( !_rxChk_INSYNC() ) return false;

        // Done
        return _rxChk_OK();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _readFlashPage(final int[] buff, final int address)
    {
        // NOTE : Flash memory can be read without page-aligned address and/or size

        final int endAddr  = Math.min(address + _config.memoryFlash.pageSize, _config.memoryFlash.totalSize);
        final int readSize = endAddr - address;

        return _readPage(buff, address, readSize, 'F');
    }

    private boolean _writeFlashPage(final int[] buff, final int address)
    { return _writePage(buff, address, _config.memoryFlash.pageSize, 'F'); }

    private int _readEEPROMByte(final int address)
    {
        final int[] buff = new int[1];

        if( !_readPage(buff, address, 1, 'E') ) return -1;

        return buff[0];
    }

    private boolean _writeEEPROMByte(final int address, final int data)
    { return _writePage( new int[] { data }, address, 1, 'E' ); }

     ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _inProgMode = false;
    private boolean _chipErased = false;

    public ProgBootSTK500(final ProgBootSTK500.Config config) throws Exception
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
        if( _getSync      (                ) == false ) return false;
        if( _getSignOn    (                ) == null  ) return false;
        if( _getParameter (PAR_STK_HW_VER  ) <  0     ) return false;
        if( _getParameter (PAR_STK_SW_MAJOR) <  0     ) return false;
        if( _getParameter (PAR_STK_SW_MINOR) <  0     ) return false;
        if( _enterProgMode(                ) == false ) return false;

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

        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_READ_SIGN;
        _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

        // Send the command and check the response
        _serialTx(_pbs_wrBuff_32I, 2);

        if( !_rxChk_INSYNC() ) return false;

        _mcuSignature = _serialRx(3);

        if( !_rxChk_OK() ) return false;

        // Done
        return _mcuSignature != null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : This function is meant to erase flash and lock bits, however, it does nothing in (almost?) all
    //        bootloader implementations that use the STK500 protocol
    @Override
    public boolean chipErase()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_chipErased) return true;

        // ##### !!! TODO : Use '_writeFlashPage()' to erase the entire flash? !!! #####

        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_CHIP_ERASE;
        _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

        // Send the command and check the response
        _setSerialRxTimeout_MS_long(); // Set a much longer Rx timeout

        _serialTx(_pbs_wrBuff_32I, 2);

        final boolean res = _rxChk_INSYNC() && _rxChk_OK();

        _setSerialRxTimeout_MS_default(); // Restore the default Rx timeout

        if(!res) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        // Set flag
        _chipErased = true;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _verifyReadFlash_impl(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
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
    { return _verifyReadFlash_impl(null, startAddress, numBytes, progressCallback) == numBytes; }

    private boolean _writeFlashPage_impl(final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
    {
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
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

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
    { return _verifyReadFlash_impl(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : Not all bootloader implementations that use the STK500 protocol support EEPROM reading and writing!

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

} // class ProgBootSTK500
