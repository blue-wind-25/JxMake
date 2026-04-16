/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.Arrays;
import java.util.function.IntConsumer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jxm.*;
import jxm.tool.fwc.*;
import jxm.xb.*;


/*
 * Standard implementation of the Chip45Boot2 protocol for programming MCUs with compatible bootloaders.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written based on the algorithms and information found from:
 *
 *     chip45boot2
 *     https://github.com/eriklins/chip45boot2
 *     https://github.com/eriklins/chip45boot2/blob/main/docs/chip45boot2_infosheet.pdf
 *     Copyright (C) 2023 Erik Lins
 *     MIT License
 */
public class ProgBootChip45B2 extends ProgBootChip45 {

    private static final String ProgClassName = "ProgBootChip45B2";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int     MaxBulkDataSize = 16;

    private static final Pattern _pmConnect      = Pattern.compile("c45b2([^\\n\\r]+)\\r?\\n\\r?>"); // c45b2 <version> \r\n\r >

    private        final boolean _rs485;
    private              boolean _inProgMode     = false;

    public ProgBootChip45B2(final ProgBootChip45B2.Config config, final boolean rs485) throws Exception
    {
        // Process the superclass
        super(ProgClassName, config);

        // Copy the RS485 flag
        _rs485 = rs485;
    }

    @Override
    public byte _flashMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    @Override
    public int _flashMemoryAlignWriteSize(final int numBytes)
    { return USB2GPIO.alignWriteSize(numBytes, _config.memoryFlash.pageSize); }

    @Override
    public byte _eepromMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private       int   _cmdLen = -1;

    private       int   _rxLen  = -1;
    private final int[] _rxBuff = new int[32];

    private int _c45Rx(final int minRead, final boolean hasTrailingResponse)
    {
        // Wait for the response
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS_DEFL);

        while(true) {
            if( serialRxUnreadCount() >= minRead ) break;
            if( tms.timeout() ) {
                USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, ProgClassName);
                return -1;
            }
        }

        // Read the response
        _rxLen = 0;

        while( serialRxUnreadCount() > 0 ) {
            final int ch = _serialRxUInt8();
            _rxBuff[_rxLen++] = ch;
            /*
            SysUtil.stdDbg().printf( "%02X : %c\n", ch & 0xFF, ( !Character.isISOControl(ch) && Character.isDefined(ch) ) ? ch : ' ' );
            //*/
        }

        // Read the response further
        if(hasTrailingResponse) {
            final Integer nch = _serialRxUInt8();
            if(nch != null) {
                _rxBuff[_rxLen++] = nch;
                while( serialRxUnreadCount() > 0 ) {
                    final int ch = _serialRxUInt8();
                    _rxBuff[_rxLen++] = ch;
                    /*
                    SysUtil.stdDbg().printf( "%02X : %c\n", ch & 0xFF, ( !Character.isISOControl(ch) && Character.isDefined(ch) ) ? ch : ' ' );
                    //*/
                }
            }
        }

        // Return the number of bytes
        return _rxLen;
    }

    private int _c45Rx(final boolean hasTrailingResponse)
    { return _c45Rx(3, hasTrailingResponse); }

    private boolean _c45Rx_chkPrompt(final boolean hasTrailingResponse)
    {
        // Read the response and check for error
        final int len = _c45Rx(hasTrailingResponse);

        if(len <= 0) return false;

        // Report an error if the final three characters do not match the expected values
        if( _rxBuff[len - 1] != '>'                              ) return false;
        if( _rxBuff[len - 2] != '\r' && _rxBuff[len - 2] != '\n' ) return false;
        if( _rxBuff[len - 3] != '\n' && _rxBuff[len - 3] != '\r' ) return false;

        // Done
        return true;
    }

    private boolean _c45Rx_chkPrompt()
    { return _c45Rx_chkPrompt(false); }

    private boolean _c45Rx_chkNull(final boolean hasTrailingResponse)
    {
        // Read the response and check for error
        final int len = _c45Rx(hasTrailingResponse);

        if(len <= 0) return false;

        // Report an error if the two three characters do not match the expected values
        if( _rxBuff[len - 1] != '\r' && _rxBuff[len - 1] != '\n' ) return false;
        if( _rxBuff[len - 2] != '\n' && _rxBuff[len - 2] != '\r' ) return false;

        // Done
        return true;
    }

    private boolean _c45Rx_chkNull()
    { return _c45Rx_chkNull(false); }

    private boolean _c45SendCmd(final boolean prompt, final boolean hasTrailingResponse, final int... cmd)
    {
        // Save the command length
        _cmdLen = cmd.length;

        // Send the command and '\n'
        _serialTx(cmd );
        _serialTx('\n');

        // Read the response and check for error
        final boolean res = prompt ? _c45Rx_chkPrompt(hasTrailingResponse) : _c45Rx_chkNull(hasTrailingResponse);

        if(!res) return false;

        // Report an error if first N characters do not match the expected values
        for(int i = 0; i < cmd.length; ++i) {
            if(_rxBuff[i         ] != cmd[i]) return false;
        }
            if(_rxBuff[cmd.length] != '+'   ) return false;

        // Done
        return true;
    }

    private boolean _c45SendCmd_chkPrompt(final int... cmd)
    { return _c45SendCmd(true , false, cmd); }

    private boolean _c45SendCmd_chkPrompt_HTR(final int... cmd)
    { return _c45SendCmd(true , true , cmd); }

    private boolean _c45SendCmd_chkNull(final int... cmd)
    { return _c45SendCmd(false, false, cmd); }

    private boolean _c45SendCmd_chkNull_HTR(final int... cmd)
    { return _c45SendCmd(false, true , cmd); }

    private boolean _c45SendBulkData(final int[] data, final boolean waitSC)
    {
        // Send the data and '\n'
        _serialTx(data);
        _serialTx('\n');

        /* Read the response and check for errors
         *     # A             '.' is sent for each successfully read and parsed line of IntelHex.
         *     # An additional '*' is sent for each successfully written flash page or EEPROM byte.
         *     # An additional '+' is sent once all operations are complete.
         */
        final int len = _c45Rx(1, false);

        if(len == 1) {
            if(_rxBuff[0] == '.') {
                if(waitSC) {
                    final Integer nch = _serialRxUInt8();
                    if(nch == null || nch != '*') return false;
                }
                return true;
            }
        }

        else if(len >= 2) {
            if( _rxBuff[0] == '.' && (_rxBuff[1] == '*' || _rxBuff[1] == '+') && _rxBuff[len - 1] == '>' ) return true;
            /*
            for(int i = 0; i < len; ++i) {
                final int ch = _rxBuff[i];
                SysUtil.stdDbg().printf( "%02X : %c\n", ch & 0xFF, ( !Character.isISOControl(ch) && Character.isDefined(ch) ) ? ch : ' ' );
            }
            //*/
        }

        // Error
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static char _n3Hex(final int value) { return FWUtil._nibble2Hexs(value >>> 12); }
    private static char _n2Hex(final int value) { return FWUtil._nibble2Hexs(value >>>  8); }
    private static char _n1Hex(final int value) { return FWUtil._nibble2Hexs(value >>>  4); }
    private static char _n0Hex(final int value) { return FWUtil._nibble2Hexs(value >>>  0); }

    private boolean _c45Cmd_go()
    { return _c45SendCmd_chkNull('g'); }

    private int _c45Cmd_er(final int address)
    {
        // Send the command and check for error
        final boolean res = _c45SendCmd_chkPrompt(
            'e', 'r', _n3Hex(address), _n2Hex(address), _n1Hex(address), _n0Hex(address)
        );

        if(!res) return -1;

        /*
        for(int i = 0; i < _rxLen; ++i) {
            final int ch = _rxBuff[i];
            SysUtil.stdDbg().printf( "%02X : %c\n", ch & 0xFF, ( !Character.isISOControl(ch) && Character.isDefined(ch) ) ? ch : ' ' );
        }
        //*/

        // Find and return the result byte
        for(int i = _cmdLen + 1; i < _rxLen; ++i) {
            final int ch = _rxBuff[i];
            if(ch != '\r' && ch != '\n') return FWUtil.hexChr2ToDec( (char) ch, (char) _rxBuff[i + 1] );
        }

        // Error
        return -1;
    }

    private boolean _c45Cmd_ew(final int address, final byte value)
    {
        // Send the command and check for error
        final boolean res = _c45SendCmd_chkPrompt_HTR(
            'e', 'w', _n3Hex(address), _n2Hex(address), _n1Hex(address), _n0Hex(address), _n1Hex(value), _n0Hex(value)
        );

        if(!res) return false;

        /*
        for(int i = 0; i < _rxLen; ++i) {
            final int ch = _rxBuff[i];
            SysUtil.stdDbg().printf( "%02X : %c\n", ch & 0xFF, ( !Character.isISOControl(ch) && Character.isDefined(ch) ) ? ch : ' ' );
        }
        //*/

        // Find and check the result byte
        for(int i = _cmdLen + 1; i < _rxLen; ++i) {
            final int ch = _rxBuff[i];
            if(ch != '\r' && ch != '\n') {
                return ( FWUtil.hexChr2ToDec( (char) ch, (char) _rxBuff[i + 1] ) == (value & 0xFF) );
            }
        }

        // Error
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _begin_impl(final String serialDevice, final int baudrate)
    {
        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Open the serial port
        if( !_openSerialPort(serialDevice, baudrate, null, true) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSerDev, ProgClassName);

        // Initialize the programmer
        if(_rs485) {

            // Reset the MCU via DTR/RTS
            _serialResetMCU_DTR_RTS();
            SysUtil.sleepMS(25);

            // Try to connect as several times
            boolean connected = false;

            for(int i = 0; i < 10 && !connected; ++i) {

                // Sync sequence to wake up bootloader and align UART framing
                for(int j = 0; j < 10; ++j) _serialTx('U');

                SysUtil.sleepMS(100);
                _serialFlush();

                _serialTx(' ');

                // Wait for any response
                final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS_MIN);

                while( ! tms.timeout() ) {
                    if( serialRxUnreadCount() > 0 ) {
                        connected = true;
                        break;
                    }
                }

            } // for

            if(!connected) return USB2GPIO.notifyError(Texts.ProgXXX_FailConnectBLTout, ProgClassName);

        } // if

        final Matcher m = _c45Connect(ProgClassName, _pmConnect, _rs485);
        if(m == null) return USB2GPIO.notifyError(Texts.ProgXXX_FailInvalidBLIVal, ProgClassName);

        final String blVer = m.group(1).trim();
        //*
        SysUtil.stdDbg().printf( "\n%s\n\n", m.group(0).trim() );
        //*/

        // Confirms synchronization between the bootloader command loop and this Java driver
        SysUtil.sleepMS(100);

        _serialTx('\n');
        if( !_c45Rx_chkPrompt() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInvalidBLIVal, ProgClassName);

        // Set flag
        _inProgMode = true;

        // Done
        return true;
    }

    @Override
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

        // Go to application
        final boolean resGo = _c45Cmd_go();

        // Close the serial port
        _closeSerialPort();

        // Clear flag
        _inProgMode = false;

        // Check for error
        if(!resGo) return USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPrgDev, ProgClassName);

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : All bootloader implementations that use the Chip45Boot2 protocol do not support signature reading!

    @Override
    public boolean supportSignature()
    { return false; }

    @Override
    public boolean readSignature()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Done
        return _mcuSignature != null; // NOTE : The signature is always null because it was never initialized
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : All bootloader implementations that use the Chip45Boot2 protocol do not support chip erase!

    @Override
    public boolean chipErase()
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : All bootloader implementations that use the Chip45Boot2 protocol do not support flash reading!

    // ##### ??? TODO : Move these Intel Hex record generator functions into a common class ??? #####

          int   _curExtSegAddress     = -1;  // :   LL  AAAA RR   DD...                  CC
    final int[] _recExtSegAddressBuff = new int[1 + 2 + 4 +  2 + (2               * 2) + 2];
    final int[] _recDataBuff          = new int[1 + 2 + 4 +  2 + (MaxBulkDataSize * 2) + 2];

                                                  /*
                                                   * :LLAAAARRCC
                                                   * :00000001FF
                                                   */
                                                  // :   LL        AA        AA        RR        CC
    final int[] _recEndBuff           = new int[] { ':', '0', '0', '0', '0', '0', '0', '0', '1', 'F', 'F' };

    private int[] _recExtSegAddress(final int laddress)
    {
        /*
         * :LLAAAARRDDDDCC
         * :02000002******
         */

        final int recordType = FWUtil.IHEX_RT_ExtendedSegmentAddress;
        final int eaddress   = laddress / FWUtil._64kiB;
        final int saddress   = eaddress * FWUtil._04kiB;
        final int saddrMSB   = (saddress >> 8) & 0xFF;
        final int saddrLSB   = (saddress     ) & 0xFF;      //      LL AA AA RR          DD        DD
        final int checksum   = FWWriter_IntelHex.calcChecksum(null, 2, 0, 0, recordType, saddrMSB, saddrLSB);

        if(_curExtSegAddress == eaddress) return null; // No need to change the segment address
        _curExtSegAddress = eaddress;

        int i = 0;
                             _recExtSegAddressBuff[i++] = ':';
        i = FWUtil._put2Hexs(_recExtSegAddressBuff, i, 2         );
        i = FWUtil._put2Hexs(_recExtSegAddressBuff, i, 0         );
        i = FWUtil._put2Hexs(_recExtSegAddressBuff, i, 0         );
        i = FWUtil._put2Hexs(_recExtSegAddressBuff, i, recordType);
        i = FWUtil._put2Hexs(_recExtSegAddressBuff, i, saddrMSB  );
        i = FWUtil._put2Hexs(_recExtSegAddressBuff, i, saddrLSB  );
            FWUtil._put2Hexs(_recExtSegAddressBuff, i, checksum  );

        /*
        for(int j = 0; j < i + 2; ++j) SysUtil.stdDbg().printf( "%c", (char) _recExtSegAddressBuff[j] );
        SysUtil.stdDbg().println();
        //*/

        return _recExtSegAddressBuff;
    }

    private int[] _recData(final int laddress, final byte[] dataBDZ)
    {
        /*
         * :LLAAAARRDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDCC
         * :10****00**********************************
         */

        final int recordType = FWUtil.IHEX_RT_Data;
        final int waddress   = laddress - (_curExtSegAddress * FWUtil._64kiB);
        final int waddrMSB   = (waddress >> 8) & 0xFF;
        final int waddrLSB   = (waddress     ) & 0xFF;      //         LL               AA        AA        RR
        final int checksum   = FWWriter_IntelHex.calcChecksum(dataBDZ, MaxBulkDataSize, waddrMSB, waddrLSB, FWUtil.IHEX_RT_Data);

        int i = 0;
                                 _recDataBuff[i++] = ':';
        i = FWUtil._put2Hexs    (_recDataBuff, i, MaxBulkDataSize);
        i = FWUtil._put2Hexs    (_recDataBuff, i, waddrMSB       );
        i = FWUtil._put2Hexs    (_recDataBuff, i, waddrLSB       );
        i = FWUtil._put2Hexs    (_recDataBuff, i, recordType     );
        i = FWUtil._putData2Hexs(_recDataBuff, i, dataBDZ        );
            FWUtil._put2Hexs    (_recDataBuff, i, checksum       );

        /*
        for(int j = 0; j < i + 2; ++j) SysUtil.stdDbg().printf( "%c", (char) _recDataBuff[j] );
        SysUtil.stdDbg().println();
        //*/

        return _recDataBuff;
    }

    private int[] _recEnd()
    { return _recEndBuff; }

    @Override
    public boolean readFlash(final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return false; }

    private boolean _writeFlashPage_impl(final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_pageSize(sa, nb, _config.memoryFlash.pageSize, ProgClassName) ) return false;

        // Get the number of chunks to be written and the current chunk address
        final int ChunkSize = Math.min(_config.memoryFlash.pageSize, MaxBulkDataSize);
        final int numChunks = nb / ChunkSize;
              int cpgAddr   = sa;

        // Send the command
        if( !_c45SendCmd_chkNull('p', 'f') ) return false;

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Reset the extended segment address
        _curExtSegAddress = -1;

        // Write the chunks
        int datIdx = 0;

        for(int p = 0; p < numChunks; ++p) {

            // ##### !!! TODO : Skip writing the page if it is blank (its contents are all 'FlashMemory_EmptyValue') !!! #####

            // Write the extended segment address as required
            final int[] xsa = _recExtSegAddress(cpgAddr);

            if(xsa != null) {
                if( !_c45SendBulkData(xsa, false) ) return false;
            }

            // Write the chunk
            final int[] rdt = _recData( cpgAddr, XCom.arrayCopy(data, datIdx, ChunkSize) );

            if( !_c45SendBulkData( rdt, ( (cpgAddr + ChunkSize) % _config.memoryFlash.pageSize ) == 0 ) ) return false;

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, ChunkSize / 2);

            // Increment the counters
            cpgAddr += ChunkSize;
            datIdx  += ChunkSize;

        } // for p

        // Write the end-of-file marker
        if( !_c45SendBulkData( _recEnd(), false ) ) return false;

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
        if( !_writeFlashPage_impl(anbr.buff, sa, anbr.nb, progressCallback) ) return false;

        // Done
        return true;
    }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return -1; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

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

        // Read the EEPROM
        return _c45Cmd_er(address);
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

        // Write the EEPROM
        return _c45Cmd_ew(address, data);
    }

} // class ProgBootChip45B2
