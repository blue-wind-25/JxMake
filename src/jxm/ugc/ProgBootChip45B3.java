/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import java.util.function.IntConsumer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jxm.*;
import jxm.xb.*;


/*
 * Standard implementation of the Chip45Boot3 protocol for programming MCUs with compatible bootloaders.
 *
 * WARNING : Chip45Boot3 bootloader with XTAE encryption enabled will accept both encrypted and
 *           unencrypted firmware, and then blindly performs decryption regardless of whether the
 *           input is actually encrypted. No validation is performed on the encryption key or the
 *           decrypted output. USE WITH CAUTION - invalid keys or incorrect input type will flash
 *           corrupted firmware without triggering errors.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written based on the algorithms and information found from:
 *
 *     chip45boot3
 *     https://github.com/eriklins/chip45boot3
 *     https://github.com/eriklins/chip45boot3/blob/main/docs/chip45boot3_Infosheet.pdf
 *     Copyright (C) 2023 ER!K
 *     MIT License
 */
public class ProgBootChip45B3 extends ProgBootChip45 {

    private static final String ProgClassName = "ProgBootChip45B3";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Set<Integer> CONTROL_BYTES   = new HashSet<>( Arrays.asList(0x02, 0x03, 0x1B) ); // STX ETX ESC

    private static final int          MaxBulkDataSize = 64;
    private static final int          BufferSize      = 1 + (1 + 1 + MaxBulkDataSize + 2) * 2 + 1; // STX [ESC]([ADR] Cmd Data... CRC CRC) ETX

    private        final int          _rs485Address;
    private              boolean      _rs485BCast;
    private        final int[]        _txrxBuff       = new int[BufferSize];

    private boolean _rs485()
    { return _rs485Address > 0; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : This CRC-16 implementation is not compatible with XModem CRC-16!

    private static final int[][] BITS_LUT = new int[256][8];

    static {
        for(int v = 0; v < 256; ++v) {
            for(int i = 0; i < 8; ++i) {
                BITS_LUT[v][i] =  ( v >> (7 - i) ) & 0x01;
            }
        }
    }

    private static int _crc16_8bit(final int crc16, final int value)
    {
        final int[] bits    = BITS_LUT[value & 0xFF];
              int   crc16_  = crc16;

              int   msbMask = -( (crc16_ >>> 15) & 0x01 ); crc16_ = ( (crc16_ << 1) | bits[0] ) ^ (msbMask & 0x1021);
                    msbMask = -( (crc16_ >>> 15) & 0x01 ); crc16_ = ( (crc16_ << 1) | bits[1] ) ^ (msbMask & 0x1021);
                    msbMask = -( (crc16_ >>> 15) & 0x01 ); crc16_ = ( (crc16_ << 1) | bits[2] ) ^ (msbMask & 0x1021);
                    msbMask = -( (crc16_ >>> 15) & 0x01 ); crc16_ = ( (crc16_ << 1) | bits[3] ) ^ (msbMask & 0x1021);
                    msbMask = -( (crc16_ >>> 15) & 0x01 ); crc16_ = ( (crc16_ << 1) | bits[4] ) ^ (msbMask & 0x1021);
                    msbMask = -( (crc16_ >>> 15) & 0x01 ); crc16_ = ( (crc16_ << 1) | bits[5] ) ^ (msbMask & 0x1021);
                    msbMask = -( (crc16_ >>> 15) & 0x01 ); crc16_ = ( (crc16_ << 1) | bits[6] ) ^ (msbMask & 0x1021);
                    msbMask = -( (crc16_ >>> 15) & 0x01 ); crc16_ = ( (crc16_ << 1) | bits[7] ) ^ (msbMask & 0x1021);

        return crc16_ & 0xFFFF;

        /*
        int crc16_ = crc16;

        for(int i = 0; i < 8; ++i) {
            final boolean msbSet = (crc16_ & 0x8000) != 0;
            crc16_ = ( (crc16_ << 1) | ( ( (value & 0xFF) >> (7 - i) ) & 0x01 ) ) & 0xFFFF;
            if(msbSet) crc16_ ^= 0x1021;
        }

        return crc16_;
        */
    }

    private static int _crc16(final int prefix1, final int prefix2, final int[] buff, final int offset, final int length)
    {
        // The initial CRC-16 value
        int crc16 = 0x84CF;

        // Calculate CRC-16 for the prefixes
        if(prefix1 >= 0) crc16 = _crc16_8bit(crc16, prefix1);
        if(prefix2 >= 0) crc16 = _crc16_8bit(crc16, prefix2);

        // Calculate CRC-16 for the data
        for(int i = offset; i < length; ++i) crc16 = _crc16_8bit(crc16, buff[i]);

        // Final flush - append 16 zero bits
        crc16 = _crc16_8bit(crc16, 0);
        crc16 = _crc16_8bit(crc16, 0);

        // Return the final result
        return crc16 & 0xFFFF;
    }

    private static int _crc16(final int prefix1, final int prefix2, final int[] buff)
    { return _crc16( prefix1, prefix2, buff, 0, (buff != null) ? buff.length : 0 ); }

    private static int _crc16(final int prefix1, final int[] buff)
    { return _crc16( prefix1, -1, buff, 0, (buff != null) ? buff.length : 0 ); }

    private static int _crc16(final int[] buff, final int length)
    { return _crc16(-1, -1, buff, 0, length); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _lastTxCmd     = -1;
    private int _lastRxDataLen = -1;

    private int _putData(final int idx, final int data)
    {
        if( CONTROL_BYTES.contains(data) ) {
            _txrxBuff[idx    ] = 0x1B                           ; // ESC
            _txrxBuff[idx + 1] = ( (data & 0xFF) + 0x80 ) & 0xFF;
            return idx + 2;
        }

        _txrxBuff[idx] = data;
        return idx + 1;
    }

    private boolean _c45Tx(final int cmd, final int[] data, final int dataLen)
    {
        // Clear the last command
        _lastTxCmd = -1;

        // Check the data size
        if( (dataLen > 0 && data == null) || (dataLen > MaxBulkDataSize) ) return false;

        // Determine the effective RS485 address
        final int rs485Addr = _rs485BCast ? 0 : _rs485Address;

        // Calculate CRC-16
        final int crc16 =  _rs485()
                        ? _crc16(rs485Addr, cmd, data)
                        : _crc16(           cmd, data);

        // Generate the buffer
        int idx = 0;
                                            _txrxBuff[idx++] = 0x02                   ; // STX

        if( _rs485() )                      idx = _putData( idx, rs485Addr           ); // Optional RS485 address

                                            idx = _putData( idx, cmd                 ); // Command
        for(int i = 0; i < dataLen; ++i)    idx = _putData( idx, data[i]             ); // Data

                                            idx = _putData( idx, (crc16 >> 8) & 0xFF ); // CRC-16
                                            idx = _putData( idx,  crc16       & 0xFF ); // ---

                                            _txrxBuff[idx++] = 0x03                   ; // ETX

        /*
        for(int i = 0; i < idx; ++i) SysUtil.stdDbg().printf("TX %02X\n", _txrxBuff[i]);
        //*/

        // Send the data
        _serialTx(_txrxBuff, idx);

        // Save the last command
        _lastTxCmd = cmd;

        // Done
        return true;
    }

    private boolean _c45Tx(final int cmd, final int[] data)
    { return _c45Tx(cmd, data, data.length); }

    private boolean _c45Tx(final int cmd)
    { return _c45Tx(cmd, null, 0); }

    private boolean _c45Rx(final int cmd, final int expectedDataLength)
    {
        int     cnt = 0;
        boolean esc = false;
        boolean stx = false;
        boolean etx = false;

        // Clear the last received data length
        _lastRxDataLen = -1;

        // Wait for the response
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS_DEFL);

        while( serialRxUnreadCount() == 0 ) {
            if( tms.timeout() ) {
                USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, ProgClassName);
                return false;
            }
        }

        // Read the response
        int dataLen = 0;

        while( serialRxUnreadCount() > 0 ) {

            // Read one byte
            final Integer ch = _serialRxUInt8();
            if(ch == null) {
                return USB2GPIO.notifyError(Texts.ProgXXX_FailBLComRxTrunc, ProgClassName);
            }

            /*
            SysUtil.stdDbg().printf("RX %02X %c\n", ch, (char) (int) ch);
            //*/

            // Check for protocol bytes
                 if(ch == 0x02) { stx = true; cnt = 0;    } // STX
            else if(ch == 0x1B) {             esc = true; } // ESC
            else if(ch == 0x03) { etx = true; break;      } // ETX
            else {
                _txrxBuff[cnt++] = esc ? ( ( (ch & 0xFF) - 0x80 ) & 0xFF ) : ch;
                esc              = false;
                 ++dataLen;
            }

            // Wait for the next byte as needed
            if( (expectedDataLength > 0) && (dataLen < expectedDataLength) ) {
                final XCom.TimeoutMS tmsd = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS_DEFL);

                while( serialRxUnreadCount() == 0 ) {
                    if( tmsd.timeout() ) {
                        USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, ProgClassName);
                        return false;
                    }
                }
            }

        } // while

        // Error if the STX or ETX is missing
        if(!stx || !etx) {
            return USB2GPIO.notifyError(Texts.ProgXXX_FailBLComFrameErr, ProgClassName);
        }

        // Calculate CRC-16 and check
        final int crc16 = _crc16(_txrxBuff, cnt - 2);

        if( ( _txrxBuff[cnt - 2] * 256 + _txrxBuff[cnt - 1] ) != crc16 ) {
            return USB2GPIO.notifyError(Texts.ProgXXX_FailBLComCRCRxErr, ProgClassName);
        }

        /*
         * Check the response
         *
         *     Normal Mode            RS485 Mode
         *         RX 02                  RX 02
         *                            0   RX ** 458_ADDR
         *     0   RX ** CMD|0x80     1   RX ** CMD|0x80
         *     1   RX ** DAT          2   RX ** DAT
         *     .   RX ** DAT          .   RX ** DAT
         *     N-2 RX ** CRC          N-2 RX ** CRC
         *     N-1 RX ** CRC          N-1 RX ** CRC
         *         RX 03                  RX 03
         */
        int idx = 0;

        if( _rs485() ) {
            // NOTE : RS485 broadcasts should not receive replies
            if( _txrxBuff[idx++] != _rs485Address ) {
                return USB2GPIO.notifyError(Texts.ProgXXX_FailBLComInv485Ad, ProgClassName);
            }
        }

        /*
         * Check for 'e0' response indicating CRC error in the command
         *
         *     Normal Mode     RS485 Mode
         *       RX 02           RX 02
         *                     0 RX ** 458_ADDR
         *     0 RX 65 DAT     1 RX 65 DAT
         *     1 RX 30 DAT     2 RX 30 DAT
         *     2 RX DF CRC     3 RX ** CRC
         *     3 RX 83 CRC     4 RX ** CRC
         *       RX 03           RX 03
         */
        if(_txrxBuff[idx] == 'e' && _txrxBuff[idx + 1] == '0') {
            return USB2GPIO.notifyError(Texts.ProgXXX_FailBLComCRCTxErr, ProgClassName);
        }

        // Check if the first response byte matches the command OR'ed with 0x80
        if( _txrxBuff[idx++] != (cmd | 0x80) ) {
            return USB2GPIO.notifyError(Texts.ProgXXX_FailBLComInvHead, ProgClassName);
        }

        // Shift the buffer left to remove '458_ADDR' and 'CMD|0x80'; CRC is excluded from the shift
        System.arraycopy(_txrxBuff, idx, _txrxBuff, 0, cnt - idx - 2);

        // Save the last received data length
        _lastRxDataLen = cnt - idx - 2;

        // Done
        return true;
    }

    private boolean _c45Rx(final int expectedDataLength)
    { return _c45Rx(_lastTxCmd, expectedDataLength); }

    private boolean _c45Rx()
    { return _c45Rx(_lastTxCmd, -1); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int[] _c45ReadVersionBL()
    {
        // Send the command and receive the response
        if( !_c45Tx(0x11) ) return null;

        if( !_c45Rx() ) return null;

        /*
         * Check the response
         *     0 RX MM
         *     1 RX NN
         */
        if(_lastRxDataLen != 2) return null;

        /*
        SysUtil.stdDbg().printf("%d.%d\n", _txrxBuff[0], _txrxBuff[1]);
        //*/

        return ( (_txrxBuff[0] + _txrxBuff[1]) > 0 ) ? ( new int[] { _txrxBuff[0], _txrxBuff[1] } ) : null;
    }

    private int[] _c45ReadVersionFW()
    {
        // Send the command and receive the response
        if( !_c45Tx(0x12) ) return null;

        if( !_c45Rx() ) return null;

        // Check and return the response
        if(_lastRxDataLen != 2) return null;

        /*
        SysUtil.stdDbg().printf("%d.%d\n", _txrxBuff[0], _txrxBuff[1]);
        //*/

        return ( new int[] { _txrxBuff[0], _txrxBuff[1] } );
    }

    private boolean _c45StartApp()
    {
        // NOTE : It seems this command must be sent in broadcast mode
        _rs485BCast = true;

        // Send the command and receive the response
        if( !_c45Tx(0x18) ) return false;

        // Discard the response; it may be truncated due to bootloader exit
        _serialDrain();

        // Done
        return true;
    }

    private boolean _c45SetAddress(final int address)
    {
        // Build the data
        final int[] data = new int[] {
            (address >>> 24) & 0xFF,
            (address >>> 16) & 0xFF,
            (address >>>  8) & 0xFF,
            (address >>>  0) & 0xFF
        };

        // Send the command and receive the response
        if( !_c45Tx(0x21, data) ) return false;

        if( !_c45Rx() ) return false;

        // Check the response
        return (_lastRxDataLen == 0);
    }

    private boolean _c45FlashWrite(final int[] buff)
    {
        // Send the command and receive the response
        if( !_c45Tx(0x22, buff) ) return false;

        if( !_c45Rx() ) return false;

        // Check the response
        return (_lastRxDataLen == 0);
    }

    private int _c45FlashRead(final int[] buff)
    {
        // Send the command and receive the response
        if( !_c45Tx( 0x23, new int[] { MaxBulkDataSize } ) ) return -1;

        if( !_c45Rx(MaxBulkDataSize) ) return -1;

        // Copy the response and return the received data length
        System.arraycopy(_txrxBuff, 0, buff, 0, _lastRxDataLen);

        return _lastRxDataLen;
    }

    private boolean _c45EEPROMWrite(final int[] buff)
    {
        // Send the command and receive the response
        if( !_c45Tx(0x24, buff) ) return false;

        if( !_c45Rx() ) return false;

        // Check the response
        return (_lastRxDataLen == 0);
    }

    private int _c45EEPROMRead(final int[] buff)
    {
        // Send the command and receive the response
        if( !_c45Tx( 0x25, new int[] { MaxBulkDataSize } ) ) return -1;

        if( !_c45Rx(MaxBulkDataSize) ) return -1;

        // Copy the response and return the received data length
        System.arraycopy(_txrxBuff, 0, buff, 0, _lastRxDataLen);

        return _lastRxDataLen;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String  _pmMagic    = "c45b3";
    private static final Pattern _pmConnect  = Pattern.compile("\\x02" + _pmMagic + "\\x03"); // STX c45b3 ETX

    private              boolean _inProgMode = false;
    private              boolean _chipErased = false;

    private              XTEA    _xtea       = null;

    public ProgBootChip45B3(final ProgBootChip45B3.Config config, final int rs485Address) throws Exception
    {
        // Process the superclass
        super(ProgClassName, config);

        // Copy the RS485 address
        _rs485Address = (rs485Address > 0) ? rs485Address : -1;
        _rs485BCast   = false;
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

    private boolean _begin_impl(final String serialDevice, final int baudrate)
    {
        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Clear flag
        _chipErased = false;

        // Open the serial port
        if( !_openSerialPort(serialDevice, baudrate, null, false) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSerDev, ProgClassName);

        // Initialize the programmer
        if( _rs485() ) {

            // Reset the MCU via DTR/RTS
            _serialResetMCU_DTR_RTS();
            SysUtil.sleepMS(25);

            // Try to connect as several times
            boolean connected = false;

            for(int i = 0; i < 10 && !connected; ++i) {

                // Sync sequence to wake up bootloader and align UART framing
                for(int j = 0; j < 16; ++j) _serialTx('U');

                // Send the target address
                _serialTx(0x80         ); // Address select initiation
                _serialTx(_rs485Address); // Send the target address

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

            // Check the address
            final Integer ch = _serialRxUInt8();
            if(ch == null || ch != _rs485Address) return USB2GPIO.notifyError(Texts.ProgXXX_FailInvalidBLIVal, ProgClassName);

            _serialTx(0x81); // End addressing sequence

            //*
            SysUtil.stdDbg().printf( "\n0x%02X %s\n\n", _rs485Address, _pmMagic );
            //*/

        }

        else {

            final Matcher m = _c45Connect(ProgClassName, _pmConnect, false);
            if(m == null) return USB2GPIO.notifyError(Texts.ProgXXX_FailInvalidBLIVal, ProgClassName);

            //*
            SysUtil.stdDbg().printf( "\n%s\n\n", m.group(0).trim() );
            //*/

        } // if

        if( _c45ReadVersionBL() == null ) return false;

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

        // Start the application
        final boolean resCommitEEPROM = commitEEPROM();
        final boolean resStartApp     = _c45StartApp();

        // Close the serial port
        _closeSerialPort();

        // Clear flag
        _inProgMode = false;

        // Check for error(s)
        if(!resCommitEEPROM || !resStartApp) {
            if(!resCommitEEPROM ) USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_CmEEPROM, ProgClassName);
            if(!resStartApp     ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPrgDev, ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    public void enableXTEA(final long[] key4x32)
    { _xtea = new XTEA( new long[] { key4x32[0], key4x32[1], key4x32[2], key4x32[3] } ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : All bootloader implementations that use the Chip45Boot3 protocol do not support signature reading!

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

    // WARNING : All bootloader implementations that use the Chip45Boot3 protocol do not support chip erase!

    @Override
    public boolean chipErase()
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : Not all bootloader implementations that use the Chip45Boot3 protocol support flash reading!

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
        final int     ChunkSize  = Math.min(_config.memoryFlash.pageSize, MaxBulkDataSize);

        final boolean notAligned = (numBytes % ChunkSize) != 0;
        final int     numChunks  = (numBytes / ChunkSize) + (notAligned ? 1 : 0);

        // Read the bytes (and compare them if requested)
        final int[] cbytes = new int[ChunkSize];

              int   rdbIdx = 0;
              int   verIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, numBytes - rdbIdx);

            if( !_c45SetAddress(sa + c * ChunkSize) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                return -1;
            }

            if( _c45FlashRead(cbytes) < numReads ) {
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

        // Determine the number of chunks
        final int ChunkSize = Math.min(_config.memoryFlash.pageSize, MaxBulkDataSize);

        final int numChunks = nb / ChunkSize;
              int cpgAddr   = sa;

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Clear flag
        _chipErased = false;

        // Write the chunks
        int datIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // ##### !!! TODO : Skip writing the page if it is blank (its contents are all 'FlashMemory_EmptyValue') !!! #####

            // Write the address as required
            if( (cpgAddr % _config.memoryFlash.pageSize) == 0 ) {
                if( !_c45SetAddress(cpgAddr) ) {
                    return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                }
            }

            // Write the chunk
            if( !_c45FlashWrite( USB2GPIO.ba2ia(data, datIdx, ChunkSize) ) ) {
                return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
            }

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, ChunkSize / 2);

            // Increment the counters
            cpgAddr += ChunkSize;
            datIdx  += ChunkSize;

        } // for c

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

        // Encrypt as needed
        if(_xtea != null) {
            final int[] encBuff = _xtea.encrypt( USB2GPIO.ba2ia(anbr.buff, 0, anbr.nb) );
            USB2GPIO.ia2ba(anbr.buff, encBuff, 0, anbr.nb);
        }

        // Write flash
        if( !_writeFlashPage_impl(anbr.buff, sa, anbr.nb, progressCallback) ) return false;

        // Done
        return true;
    }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash_impl(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : Not all bootloader implementations that use the Chip45Boot3 protocol support EEPROM reading and writing!

    private int[]     _eepromBuffer = null;
    private boolean[] _eepromFDirty = null;

    private boolean _readAllEEPROMBytes()
    {
        // Prepare the result buffer
        if(_eepromBuffer == null) _eepromBuffer = new int[_config.memoryEEPROM.totalSize];

        // Determine the chunk size and the number of chunks
        final int ChunkSize = Math.min(_config.memoryEEPROM.totalSize, MaxBulkDataSize);
        final int numChunks = (_config.memoryEEPROM.totalSize / ChunkSize);

        // Read the bytes
        final int[] cbytes = new int[ChunkSize];
              int   rdbIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk
            final int numReads = Math.min(ChunkSize, _config.memoryEEPROM.totalSize - rdbIdx);

            // Read the chunk bytes
            if( !_c45SetAddress(c * ChunkSize) ) {
                return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
            }

            if( _c45EEPROMRead(cbytes) < numReads ) {
                return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
            }

            // Store the bytes to the result buffer
            for(int b = 0; b < numReads; ++b) _eepromBuffer[rdbIdx++] = cbytes[b];

        } // for c

        // Done
        return true;
    }

    private boolean _writeAllEEPROMBytes()
    {
        // Determine the chunk size and the number of chunks
        final int ChunkSize = Math.min(_config.memoryEEPROM.totalSize, MaxBulkDataSize);
        final int numChunks = (_config.memoryEEPROM.totalSize / ChunkSize);

        // Write the bytes
        int wdbIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Write in chunk
            final int numWrites = Math.min(ChunkSize, _config.memoryEEPROM.totalSize - wdbIdx);

            // Check if the chunk is dirty
            boolean chunkDirty = false;

            for(int b = 0; b < numWrites; ++b) {
                if(_eepromFDirty[c * ChunkSize + b]) {
                    chunkDirty = true;
                    break;
                }
            }

            // Do not write if the chunk is not dirty
            if(!chunkDirty) {
                wdbIdx += numWrites;
                continue;
            }

            // Copy the bytes to the write buffer
            final int[] cbytes = new int[numWrites];

            for(int b = 0; b < numWrites; ++b) cbytes[b] = _eepromBuffer[wdbIdx++];

            // Write the bytes
            if( !_c45SetAddress(c * ChunkSize) ) {
                return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
            }

            if( !_c45EEPROMWrite(cbytes) ) {
                return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
            }

        } // for c

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

} // class ProgBootChip45B3
