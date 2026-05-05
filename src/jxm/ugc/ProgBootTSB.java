/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;

import java.util.Arrays;
import java.util.function.IntConsumer;

import jxm.*;
import jxm.xb.*;


/*
 * Minimal implementation of the TinySafeBoot protocol for programming MCUs with compatible bootloaders.
 * Limitations:
 *     1. Dynamixel-based connections are not supported.
 *     2. Emergency erase functionality is not available.
 *     3. Password support is limited; the password is only updated when the application flash section
 *        is written
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written partially based on the algorithms and information found from:
 *
 *     Tinysafeboot Bootloader
 *     https://kb.seedrobotics.com/doku.php?id=tsb:home
 *
 *     tinysafeboot
 *     https://github.com/seedrobotics/tinysafeboot/tree/master
 *     Written in 2011-2015 by Julien Thomas
 *     Extended by Seed Robotics from 2017
 *
 * JxMake reimplements protocol behavior based on runtime observation and bench-level validation.
 * No TinySafeBoot source code or expressive implementation logic is used. Constants and timing are
 * derived from functional analysis and do not constitute derivative work.
 */
public class ProgBootTSB extends ProgBootSerial {

    private static final String ProgClassName = "ProgBootTSB";

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

    private static enum AppJumpMode {
        None,     // Using boot reset vector
        Relative, // Using relative jump
        Absolute  // Using absolute jump
    }

    private static final int         TSB_CONFIRM_CHAR      = '!';
    private static final int         TSB_REQUEST_CHAR      = '?';

    private static final int         TSB_STOPRES_CHAR      = ' '; // NOTE : Any character other than TSB_CONFIRM_CHAR will do

    private static final int         TSB_ACTIVATION_MSGLEN = 17 - 1; // Excluding the TSB_CONFIRM_CHAR

    private              boolean     _noRxSkip             = false;
    private        final int[]       _txrxBuff             = new int[2 * 1024 * 1024]; // 2MiB is more than enough for the largest AVR

    private              int         _magic                = -1;
    private              int         _buildDate            = -1;
    private              int         _buildState           = -1;
    private              int         _pageSize             = -1;
    private              int         _appFlashEnd          = -1;
    private              int         _appFlashSize         = -1;
    private              int         _eepromSize           = -1;
    private              AppJumpMode _appJumpMode          = AppJumpMode.None;
    private              int         _appJumpAddress       = -1;
    private              int         _blTimeout            = -1;

    private boolean _skip(final int skipLen)
    {
        // ##### ??? TODO : Why some TinySafeBoot implementation echoes back the Tx ??? #####

        if(!_noRxSkip && skipLen != 0) {
            if( !_serialRx(_txrxBuff, skipLen) ) return false;
        }

        return true;
    }

    private boolean _read(final int skipLen, final int readLen)
    {
        if( !_skip(skipLen) ) return false;

        if( !_serialRx(_txrxBuff, readLen) ) return false;

        return true;
    }

    private boolean _readUntil_CONFIRM(final int skipLen, final int readLen)
    {
        if( !_read(skipLen, readLen + 1) ) return false;

        return (_txrxBuff[readLen] == TSB_CONFIRM_CHAR);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int GET_SYNC_RETRY_COUNT = 8; // Must be a multiple of 4

    private boolean _connect()
    {
        // Prepare the autobaud/activation characters
        _pbs_wrBuff_32I[0] = '@';
        _pbs_wrBuff_32I[1] = '@';
        _pbs_wrBuff_32I[2] = '@';

        final int lenNoPswd   = 3;
        final int lenWithPswd = (_curPassword != null) ? (_curPassword.length + lenNoPswd) : 0;

        if(lenWithPswd != 0) {
            for(int i = 0; i < _curPassword.length; ++i) _pbs_wrBuff_32I[lenNoPswd + i] = _curPassword[i];
        }

        // Send the command as several times
        for(int i = 0; i < GET_SYNC_RETRY_COUNT; ++i) {

            // Reset the MCU via DTR/RTS
            _serialResetMCU_DTR_RTS();
            SysUtil.sleepMS(25);

            /*
            // Disable Rx skip when n matches the second half of each 4-count block (i.e., n is 2, 3, 6, 7, ...)
            _noRxSkip = (i & 0x03) >= 0x02;
            */
            // Disable Rx skip when n matches the first half of each 4-count block (i.e., n is 0, 1, 4, 5, ...)
            _noRxSkip = (i & 0x03) <= 0x01;

            // Send the autobaud/activation characters, plus the password on every odd counter (if the password is specified)
            final int sendLen = ( lenWithPswd != 0 && (i & 0x01) != 0 ) ? lenWithPswd : lenNoPswd;
            _serialTx(_pbs_wrBuff_32I, sendLen);

            // Check the response
            if( _readUntil_CONFIRM(sendLen, TSB_ACTIVATION_MSGLEN) ) {
                // Parse the response
                /*
                for(int r = 0; r < TSB_ACTIVATION_MSGLEN; ++r) SysUtil.stdDbg().printf("[%02d] %02X %c\n", r, _txrxBuff[r], _txrxBuff[r]);
                //*/
                _magic        =   _txrxBuff[ 2] | (_txrxBuff[ 1] << 8) | (_txrxBuff[0] << 16);
                _buildDate    =   _txrxBuff[ 3] | (_txrxBuff[ 4] << 8);
                _buildState   =   _txrxBuff[ 5];
                _mcuSignature = new int[] { _txrxBuff[6], _txrxBuff[7], _txrxBuff[8] };
                _pageSize     =   _txrxBuff[ 9] * 2;
                _appFlashEnd  = ( _txrxBuff[10] | (_txrxBuff[11] << 8) ) * 2;
                _appFlashSize = ( (_appFlashEnd / 1024) + 1 ) * 1024;
                _eepromSize   = ( _txrxBuff[12] | (_txrxBuff[13] << 8) ) + 1;
                _appJumpMode  = ( _txrxBuff[14] == 0x00 ) ? AppJumpMode.Relative
                              : ( _txrxBuff[14] == 0x0C ) ? AppJumpMode.Absolute
                              :                             AppJumpMode.None;
                // Check the page size and EEPROM size
                if(_pageSize   != _config.memoryFlash.pageSize  ) return false;
                if(_eepromSize != _config.memoryEEPROM.totalSize) return false;
                // Done
                return true;
            }

            // Delay before retry attempt
            _ttyPort.clearDTR();
            _ttyPort.clearRTS();
            SysUtil.sleepMS(500);

        } // for

        // Not done
        return false;

    }

    private boolean _disconnect()
    {
        _serialTx('x'); // NOTE : Sending any invalid command character triggers bootloader exit

        return true;
    }

    private boolean _readLastPage()
    {
        // Read the last page
        _serialTx('c');

        if( !_readUntil_CONFIRM(1, _pageSize) ) return false;

        /*
        for(int r = 0; r < _pageSize; ++r) SysUtil.stdDbg().printf("[%03d] %02X %c\n", r, _txrxBuff[r], _txrxBuff[r]);
        //*/

        // Get the application jump address and bootloader timeout
       _appJumpAddress = _txrxBuff[0] | (_txrxBuff[1] << 8);
       _blTimeout      = _txrxBuff[2];

        // Done
        return true;
    }

    private boolean _writeLastPage()
    {
        // Generate the data
        final int[] buff = new int[_pageSize];

        for(int i = 0; i < _pageSize; ++i) buff[i] = FlashMemory_EmptyValue & 0xFF;

        buff[0] = (_appJumpAddress >> 0) & 0xFF;
        buff[1] = (_appJumpAddress >> 8) & 0xFF;

        buff[2] = _blTimeout;

        if(_newPassword != null) {
            for(int i = 0; i < _newPassword.length; ++i) buff[3 + i] = _newPassword[i];
        }

        // Write the data
        if(true) {

            // Send the initial command
            _serialTx('C');

            if( !_skip(1) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            // Wait for TSB_REQUEST_CHAR
            final Integer reqChar = _serialRxUInt8();
            if(reqChar == null || reqChar != TSB_REQUEST_CHAR) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            // Send confirm and write the bytes
            _serialTx(TSB_CONFIRM_CHAR);

            _serialTx(buff);

        } // if

        // Verify the data
        if( !_readUntil_CONFIRM(1 + _pageSize, _pageSize) ) return false;


        for(int i = 0; i < _pageSize; ++i) {
             if(_txrxBuff[i] != buff[i]) return false;
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[]   _curPassword = null;
    private int[]   _newPassword = null;

    private boolean _inProgMode  = false;

    public ProgBootTSB(final ProgBootTSB.Config config) throws Exception
    {
        // Process the superclass
        super(ProgClassName, config);
    }

    @Override
    public byte _flashMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    @Override
    public int _flashMemoryAlignWriteSize(final int numBytes)
    { return USB2GPIO.alignWriteSize(numBytes, _pageSize); }

    @Override
    public byte _eepromMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    public void setPassword(final String curPassword, final String newPassword)
    {
        // NOTE : This function must be called before 'begin()'

        _curPassword = null;
        _newPassword = null;

        if( curPassword != null && !curPassword.isEmpty() ) {
            final byte[] utf8Bytes = curPassword.getBytes(SysUtil._CharSet);
            final int    len       = Math.min(utf8Bytes.length, 16);
            _curPassword = new int[len];
            for(int i = 0; i < len; ++i) _curPassword[i] = utf8Bytes[i] & 0xFF;
        }

        if( newPassword != null && !newPassword.isEmpty() ) {
            final byte[] utf8Bytes = newPassword.getBytes(SysUtil._CharSet);
            final int    len       = Math.min(utf8Bytes.length, 16);
            _newPassword = new int[len];
            for(int i = 0; i < len; ++i) _newPassword[i] = utf8Bytes[i] & 0xFF;
        }
    }

    private boolean _begin_impl(final String serialDevice, final int baudrate)
    {
        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Reset flag
        _noRxSkip = false;

        // Open the serial port
        if( !_openSerialPort(serialDevice, baudrate, null) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSerDev, ProgClassName);

        // Initialize the programmer
        if( !_connect     () ) return false;
        if( !_readLastPage() ) return false;

        //*
        // Print some message
        final int nameLen = 15;
        SysUtil.stdDbg().printf( "\n-------------------------------\n" );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, "TSB"                         , "%06X"      ), _magic                           );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, Texts.CmdXInf_BLBuildDate     , "%04X"      ), _buildDate                       );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, Texts.CmdXInf_BLBuildState    , "%02X"      ), _buildState                      );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, Texts.CmdXInf_BLFlashPageSize , "%03d"      ), _pageSize                        );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, Texts.CmdXInf_BLEEPROMSize    , "%04d"      ), _eepromSize                      );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, Texts.CmdXInf_BLAppFlashEnd   , "%04X (%6d)"), _appFlashEnd   , _appFlashEnd    );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, Texts.CmdXInf_BLAppFlashSize  , "%04X (%6d)"), _appFlashSize  , _appFlashSize   );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, Texts.CmdXInf_BLAppJumpMode   , "%s"        ), _appJumpMode.name()              );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, Texts.CmdXInf_BLAppJumpAddress, "%04X (%6d)"), _appJumpAddress, _appJumpAddress );
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific("", nameLen, Texts.CmdXInf_BLTimeoutCounter, "%03d      "), _blTimeout     , _blTimeout      );
        SysUtil.stdDbg().printf( "-------------------------------\n\n" );
        //*/

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

        // Commit EEPROM and leave programming mode
        final boolean resCommitEEPROM = commitEEPROM();
        final boolean resDisconnect   = _disconnect();

        // Close the serial port
        _closeSerialPort();

        // Clear flag
        _inProgMode = false;

        // Check for error(s)
        if(!resCommitEEPROM || !resDisconnect) {
            if(!resCommitEEPROM) USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_CmEEPROM, ProgClassName);
            if(!resDisconnect  ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPrgDev, ProgClassName);
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

        // NOTE : The MCU signature was read during 'begin()'

        // Done
        return _mcuSignature != null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : This bootloader implementation performs automatic chip erase

    @Override
    public boolean chipErase()
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : This bootloader implementation can only read from and write to the entire application flash section

    private int _verifyReadFlash_impl(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Determine the start address and number of bytes
        final int sa = (startAddress < 0) ? 0                             : startAddress;
        final int nb = (numBytes     < 0) ? (_appFlashEnd - startAddress) : (sa + numBytes);

        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_even(sa, nb, _config.memoryFlash.totalSize, ProgClassName) ) return -1;

        // Prepare the result buffer
        if(_config.memoryFlash.readDataBuff == null || _config.memoryFlash.readDataBuff.length != nb) {
            _config.memoryFlash.readDataBuff = new int[nb];
        }

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Determine the number of pages
        final boolean notAligned = (nb % _pageSize) != 0;
        final int     numPages   = (nb / _pageSize) + (notAligned ? 1 : 0);

              int     curAdr     = 0;
              int     rdbIdx     = 0;
              int     verIdx     = 0;

        // Send the initial command
        _serialTx('f');

        // Read the bytes (and compare them if requested)
        for(int i = 0; i < numPages; ++i) {

            // Send confirm and read the bytes
            _serialTx(TSB_CONFIRM_CHAR);

            if( !_read( (i == 0) ? 2 : 1, _pageSize ) ) {
                if( (curAdr + _pageSize) < _appFlashEnd ) return -1;
                else                                      i = numPages;
            }

            // Restore the modified reset vector
            if(curAdr == 0 && _appJumpMode == AppJumpMode.Absolute) {
                // NOTE : It seems the only legacy ATtiny devices with >= 16kB of flash are the ATtiny167 and ATtiny1634
                // ##### !!! TODO : Is this correct? !!! #####
                _txrxBuff[2] = (_appJumpAddress >> 0) & 0xFF;
                _txrxBuff[3] = (_appJumpAddress >> 8) & 0xFF;
            }
            else if(curAdr == 0 && _appJumpMode == AppJumpMode.Relative) {
                final int pageSizeW   = _pageSize     / 2;
                final int flashSizeW  = _appFlashSize / 2;
                final int flashDeltaW = 4096 - flashSizeW;
                final int origRstVec = ( (_appJumpAddress - 0xC000)   > flashDeltaW )
                                     ? (  _appJumpAddress - pageSizeW - flashDeltaW )  // TinySafeBoot with backward RJMP
                                     : (  _appJumpAddress - pageSizeW               ); // TinySafeBoot with forward  RJMP
                _txrxBuff[0] = (origRstVec >> 0) & 0xFF;
                _txrxBuff[1] = (origRstVec >> 8) & 0xFF;
            }

            // Process the page bytes
            for(int b = 0; b < _pageSize; b += 2) {

                if(curAdr >= startAddress && rdbIdx < nb) {

                    // Store the bytes to the result buffer
                    _config.memoryFlash.readDataBuff[rdbIdx++] = _txrxBuff[b    ];
                    _config.memoryFlash.readDataBuff[rdbIdx++] = _txrxBuff[b + 1];

                    // Compare the bytes as needed
                    if(refData != null && verIdx < refData.length) {
                        if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                        ++verIdx;
                        if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                        ++verIdx;
                    }

                    // Call the progress callback function for the current value
                    pcb.callProgressCallbackCurrent(progressCallback, nb);

                } // if

                //
                curAdr += 2;

            } // for b

        } // for i

        // Cancel further page read and check for confirmation
        if(curAdr < _appFlashEnd) {
            _serialTx(TSB_STOPRES_CHAR);
            if( !_readUntil_CONFIRM(1, 0) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                return -1;
            }
        }
        // Only check for confirmation
        else {
            if( !_readUntil_CONFIRM(0, 0) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                return -1;
            }
        }

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
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_pageSize(sa, nb, _pageSize, ProgClassName) ) return false;

        if(sa != 0) return USB2GPIO.notifyError(Texts.ProgXXX_SAddrNotZero , ProgClassName); // The start address must be zero

        // Get the number of pages to be written
        final int numPages = nb / _pageSize;

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Send the initial command
        _serialTx('F');

        if( !_skip(1) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        SysUtil.sleepMS( Math.max(100, _appFlashSize / 250) ); // Wait for the erase to complete

        // Write the pages
        int curAdr = 0;
        int datIdx = 0;

        for(int p = 0; p < numPages; ++p) {

            // NOTE : Cannot skip writing the page even if it is blank (its contents are all 'FlashMemory_EmptyValue')

            // Wait for TSB_REQUEST_CHAR
            final Integer reqChar = _serialRxUInt8();
            if(reqChar == null || reqChar != TSB_REQUEST_CHAR) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            // Send confirm and write the bytes
            _serialTx(TSB_CONFIRM_CHAR);

            _serialTx( USB2GPIO.ba2ia(data, datIdx, _pageSize) );

            if( !_skip(1 + _pageSize) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, _pageSize / 2);

            // Increment the counters
            curAdr += _pageSize;
            datIdx += _pageSize;

        } // for p

        // Cancel further page write and check for confirmation
        if(curAdr < _appFlashEnd) {
            _serialRxUInt8(); // Discard the TSB_REQUEST_CHAR
            _serialTx(TSB_STOPRES_CHAR);
            if( !_readUntil_CONFIRM(1, 0) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
        }
        // Only check for confirmation
        else {
            if( !_readUntil_CONFIRM(0, 0) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
        }

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
        final int sa = (startAddress < 0) ? 0            : startAddress;
        final int nb = (numBytes     < 0) ? _appFlashEnd : numBytes;

        // Align the number of bytes and pad the buffer as needed
        final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer(data, sa, nb, _config.memoryFlash.pageSize, _appFlashEnd, FlashMemory_EmptyValue, ProgClassName);
        if(anbr == null) return false;

        // Write flash
        if( !_writeFlashPage_impl(anbr.buff, sa, anbr.nb, progressCallback) ) return false;

        if( !_writeLastPage() ) return false;

        // Done
        return true;
    }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash_impl(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : This bootloader implementation can only read from and write to the entire EEPROM

    private int[]     _eepromBuffer = null;
    private boolean[] _eepromFDirty = null;

    private boolean _readAllEEPROMBytes()
    {
        // Clear the result buffer
        _eepromBuffer = null;

        // Prepare the read buffer
        final int[] eepromBuffer = new int[_config.memoryEEPROM.totalSize];

        // Determine the number of pages
        final int numPages = _config.memoryEEPROM.totalSize / _pageSize;

        // Send the initial command
        _serialTx('e');

        // Read the bytes (and compare them if requested)
        int rdbIdx = 0;

        for(int i = 0; i < numPages; ++i) {

            // Send confirm and read the bytes
            _serialTx(TSB_CONFIRM_CHAR);

            if( !_read( (i == 0) ? 2 : 1, _pageSize ) ) return false;

            // Store the bytes to the read buffer
            for(int b = 0; b < _pageSize; ++b) eepromBuffer[rdbIdx++] = _txrxBuff[b];

        } // for i

        // Stop the page read and check for confirmation
        _serialTx(TSB_STOPRES_CHAR);

        if( !_readUntil_CONFIRM(1, 0) ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
            return false;
        }

        // Set the result buffer
        _eepromBuffer = eepromBuffer;

        // Done
        return true;
    }

    private boolean _writeAllEEPROMBytes()
    {
        // Get the number of pages to be written
        final int numPages = _config.memoryEEPROM.totalSize / _pageSize;

        // Send the initial command
        _serialTx('E');

        if( !_skip(1) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        // Write the pages
        int datIdx = 0;

        for(int p = 0; p < numPages; ++p) {

            // Wait for TSB_REQUEST_CHAR
            final Integer reqChar = _serialRxUInt8();
            if(reqChar == null || reqChar != TSB_REQUEST_CHAR) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            // Send confirm and write the bytes
            _serialTx(TSB_CONFIRM_CHAR);

            _serialTx( XCom.arrayCopy(_eepromBuffer, datIdx, _pageSize) );

            if( !_skip(1 + _pageSize) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            // Increment the counter
            datIdx += _pageSize;

        } // for p

        // Stop the page write and check for confirmation
        _serialRxUInt8(); // Discard the TSB_REQUEST_CHAR

        _serialTx(TSB_STOPRES_CHAR);

        if( !_readUntil_CONFIRM(1, 0) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        // Done
        return true;
    }

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

        // Read the entire EEPROM as needed
        if(_eepromBuffer == null) {
            // Read the bytes
            if( !_readAllEEPROMBytes() ) return -1;
            // Mark everything as not dirty
            _eepromFDirty = new boolean[_config.memoryEEPROM.totalSize];
            Arrays.fill(_eepromFDirty, false);
        }

        // Return the byte
        return _eepromBuffer[address];
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

        // The EEPROM must be written in page mode, hence, read the entire EEPROM first as needed
        if(_eepromBuffer == null) {
            if( readEEPROM(0) < 0 ) return false;
        }

        // Store the new byte to the buffer and mark the position as dirty
        final int newData = data & 0xFF;

        if(_eepromBuffer[address] != newData) {
            _eepromBuffer[address] = newData;
            _eepromFDirty[address] = true;
        }

        // Done
        return true;
    }

    public boolean commitEEPROM()
    {
        // Simply exit if there is no EEPROM buffer
        if(_eepromBuffer == null) return true;

        // Skip commit if no EEPROM position is marked dirty
        boolean anyDirty = false;

        for(boolean b : _eepromFDirty) {
            if(b) {
                anyDirty = true;
                break;
            }
        }

        if(!anyDirty) return true;

        // Write the entire EEPROM
        _writeAllEEPROMBytes();

        // Mark everything as not dirty
        Arrays.fill(_eepromFDirty, false);

        /*
        _eepromBuffer = null;
        //*/

        // Done
        return true;
    }

} // class ProgBootTSB
