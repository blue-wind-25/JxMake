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
 * Minimal implementation of the OpenBLT protocol for programming MCUs with compatible bootloaders.
 * Limitations:
 *     1. Only UART transport is supported (although a UART-over-network transparent passthrough
 *        bridge may work).
 *     2. Only XCP connection mode 0 (normal) is supported.
 *     3. No configurable timeout for specific operations/commands.
 *     4. No support for NVM checksum, CRC, or encryption.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written partially based on the algorithms and information found from:
 *
 *     OpenBLT - Bootloader Design
 *     https://www.feaser.com/openblt/doku.php?id=manual:design
 *
 *     XCP 1.0 Protocol Specification
 *     https://www.feaser.com/openblt/lib/exe/fetch.php?media=manual:xcp_1_0_specification.zip
 *
 *     OpenBLT SourceForge SVN (Primary)
 *     https://sourceforge.net/projects/openblt
 *     https://sourceforge.net/p/openblt/code/HEAD/tree/trunk
 *
 *     OpenBLT GitHub (Mirror)
 *     https://github.com/feaser/openblt
 *
 *     OpenBLT Running on Bluepill Plus and Blackpill Boards
 *     https://github.com/razielgdn/black-and-blue-pill-plus-with-openBLT
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * JxMake reimplements protocol behavior based on available documentation, supplemented by runtime
 * observation and bench-level validation where features are undocumented or ambiguous. No OpenBLT
 * source code or expressive implementation logic is used. Constants and timing are derived from
 * functional analysis and do not constitute derivative work.
 */
public class ProgBootOpenBLT extends ProgBootSerial {

    private static final String ProgClassName = "ProgBootOpenBLT";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class Config extends ProgBootSerial.Config {

        // NOTE : This class can use the same configuration as certain other classes
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!


// ##### !!! TODO : VERIFY !!! #####

/*
 * NOTE : # This file is used by more than one 'ProgBoot*' implementations. However, many AN3155
 *          implementations can only handle a maximum of 256 flash pages. Therefore, if both the
 *          physical page size and erase page size are specified in the datasheet or reference manual,
 *          the larger value must be assigned to 'config.memoryFlash.pageSize'.
 *        # The function naming convention is:
 *              'STM32_<flash_total_size>_<flash_page_size>()'
 *          Example:
 *              'STM32_64k_1k()'
 *          indicates 65,536 bytes total flash with 1,024-byte physical/erase page size.
 */

private static Config _genConfig(final int flashSize_kB, final int pageSize_byte)
{
    final Config config = new Config();

    config.memoryFlash.totalSize = flashSize_kB * 1024;
    config.memoryFlash.pageSize  = pageSize_byte;
    config.memoryFlash.numPages  = config.memoryFlash.totalSize / config.memoryFlash.pageSize;

    return config;
}

public static Config STM32_8k_128B  () { return _genConfig(   8,        128); }
public static Config STM32_8k_256B  () { return _genConfig(   8,        256); }
public static Config STM32_8k_512B  () { return _genConfig(   8,        512); }
public static Config STM32_8k_1k    () { return _genConfig(   8,   1 * 1024); }
public static Config STM32_8k_2k    () { return _genConfig(   8,   2 * 1024); }

public static Config STM32_16k_128B () { return _genConfig(  16,        128); }
public static Config STM32_16k_256B () { return _genConfig(  16,        256); }
public static Config STM32_16k_512B () { return _genConfig(  16,        512); }
public static Config STM32_16k_1k   () { return _genConfig(  16,   1 * 1024); }
public static Config STM32_16k_2k   () { return _genConfig(  16,   2 * 1024); }

public static Config STM32_32k_128B () { return _genConfig(  32,        128); }
public static Config STM32_32k_256B () { return _genConfig(  32,        256); }
public static Config STM32_32k_512B () { return _genConfig(  32,        512); }
public static Config STM32_32k_1k   () { return _genConfig(  32,   1 * 1024); }
public static Config STM32_32k_2k   () { return _genConfig(  32,   2 * 1024); }
public static Config STM32_32k_4k   () { return _genConfig(  32,   4 * 1024); }

public static Config STM32_64k_256B () { return _genConfig(  64,        256); }
public static Config STM32_64k_512B () { return _genConfig(  64,        512); }
public static Config STM32_64k_1k   () { return _genConfig(  64,   1 * 1024); }
public static Config STM32_64k_2k   () { return _genConfig(  64,   2 * 1024); }
public static Config STM32_64k_4k   () { return _genConfig(  64,   4 * 1024); }

public static Config STM32_128k_512B() { return _genConfig( 128,        512); }
public static Config STM32_128k_1k  () { return _genConfig( 128,   1 * 1024); }
public static Config STM32_128k_2k  () { return _genConfig( 128,   2 * 1024); }
public static Config STM32_128k_4k  () { return _genConfig( 128,   4 * 1024); }
public static Config STM32_128k_8k  () { return _genConfig( 128,   8 * 1024); }

public static Config STM32_256k_1k  () { return _genConfig( 256,   1 * 1024); }
public static Config STM32_256k_2k  () { return _genConfig( 256,   2 * 1024); }
public static Config STM32_256k_4k  () { return _genConfig( 256,   4 * 1024); }
public static Config STM32_256k_8k  () { return _genConfig( 256,   8 * 1024); }
public static Config STM32_256k_16k () { return _genConfig( 256,  16 * 1024); }
public static Config STM32_256k_32k () { return _genConfig( 256,  32 * 1024); }

public static Config STM32_512k_2k  () { return _genConfig( 512,   2 * 1024); }
public static Config STM32_512k_4k  () { return _genConfig( 512,   4 * 1024); }
public static Config STM32_512k_8k  () { return _genConfig( 512,   8 * 1024); }
public static Config STM32_512k_16k () { return _genConfig( 512,  16 * 1024); }
public static Config STM32_512k_32k () { return _genConfig( 512,  32 * 1024); }

public static Config STM32_1M_4k    () { return _genConfig(1024,   4 * 1024); }
public static Config STM32_1M_8k    () { return _genConfig(1024,   8 * 1024); }
public static Config STM32_1M_16k   () { return _genConfig(1024,  16 * 1024); }
public static Config STM32_1M_32k   () { return _genConfig(1024,  32 * 1024); }
public static Config STM32_1M_64k   () { return _genConfig(1024,  64 * 1024); }

public static Config STM32_2M_8k    () { return _genConfig(1024,   8 * 1024); }
public static Config STM32_2M_16k   () { return _genConfig(1024,  16 * 1024); }
public static Config STM32_2M_32k   () { return _genConfig(1024,  32 * 1024); }
public static Config STM32_2M_64k   () { return _genConfig(1024,  64 * 1024); }
public static Config STM32_2M_128k  () { return _genConfig(1024, 128 * 1024); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    @FunctionalInterface
    public interface UserCallback {

        public static final int REQ_FLASH_BASE_ADDR = 1; // 'requestParam[]' = null   ; return[0] = <flash_base_address>
        public static final int REQ_BOOTLOADER_SIZE = 2; // 'requestParam[]' = null   ; return[0] = <bootloader_size>
        public static final int REQ_UPROG_VTAB_SIZE = 3; // 'requestParam[]' = null   ; return[0] = <user_program_vector_table_size>

        public static final int REQ_KEY_FROM_SEED   = 9; // 'requestParam[]' = <seed> ; return[ ] = [<key>]

        public long[] handle(final int requestType, final long[] requestParam);

    } // interface UserCallback

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int XCP_CMD_CONNECT                = 0xFF;
    private static final int XCP_CMD_DISCONNECT             = 0xFE;
    private static final int XCP_CMD_GET_STATUS             = 0xFD;
    private static final int XCP_CMD_SYNCH                  = 0xFC;

    private static final int XCP_CMD_GET_COMM_MODE_INFO     = 0xFB;
    private static final int XCP_CMD_GET_ID                 = 0xFA;
    private static final int XCP_CMD_SET_REQUEST            = 0xF9;
    private static final int XCP_CMD_GET_SEED               = 0xF8;
    private static final int XCP_CMD_UNLOCK                 = 0xF7;
    private static final int XCP_CMD_SET_MTA                = 0xF6;
    private static final int XCP_CMD_UPLOAD                 = 0xF5;
    private static final int XCP_CMD_SHORT_UPLOAD           = 0xF4;
    private static final int XCP_CMD_BUILD_CHECKSUM         = 0xF3;
    private static final int XCP_CMD_TRANSPORT_LAYER_CMD    = 0xF2;
    private static final int XCP_CMD_USER_CMD               = 0xF1;

    private static final int XCP_CMD_PROGRAM_START          = 0xD2;
    private static final int XCP_CMD_PROGRAM_CLEAR          = 0xD1;
    private static final int XCP_CMD_PROGRAM                = 0xD0;
    private static final int XCP_CMD_PROGRAM_RESET          = 0xCF;
    private static final int XCP_CMD_GET_PGM_PROCESSOR_INFO = 0xCE;
    private static final int XCP_CMD_GET_SECTOR_INFO        = 0xCD;
    private static final int XCP_CMD_PROGRAM_PREPARE        = 0xCC;
    private static final int XCP_CMD_PROGRAM_FORMAT         = 0xCB;
    private static final int XCP_CMD_PROGRAM_NEXT           = 0xCA;
    private static final int XCP_CMD_PROGRAM_MAX            = 0xC9;
    private static final int XCP_CMD_PROGRAM_VERIFY         = 0xC8;

    private static final int XCP_CMD_PID_RES                = 0xFF;
    private static final int XCP_CMD_PID_ERR                = 0xFE;
    private static final int XCP_CMD_PID_EV                 = 0xFD;
    private static final int XCP_CMD_PID_SERV               = 0xFC;

    private static final int XCP_RES_PGM                    = 0x10;
    private static final int XCP_RES_STIM                   = 0x08;
    private static final int XCP_RES_DAQ                    = 0x04;
    private static final int XCP_RES_CALPAG                 = 0x01;

    private static final int XCP_USR_CMD_GET_IT             = 0x17;
    private static final int XCP_USR_IT_CID_GETINFO         = 0x04;
    private static final int XCP_USR_IT_CID_DOWNLOAD        = 0x06;
    private static final int XCP_USR_IT_CID_CHECNFO         = 0x08;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int   BufferSize              = 256;

    private static final int   GET_CONNECT_RETRY_COUNT = 3;

    private        final int[] _txrxBuff               = new int[BufferSize];
    private              int   _rxLastLen              = -1;

    private void _uartTx(final int[] txBuff) // Tx from the given buffer
    {
        // Clear the Rx length
        _rxLastLen = -1;

        // Calculate the checksum if required
        int cs = txBuff.length;

        if(_uartCS) for(final int v : txBuff) cs = (cs + v) & 0xFF;

        // Send the data
                    _serialTx(txBuff.length);
                    _serialTx(txBuff       );
        if(_uartCS) _serialTx(cs           );
    }

    private boolean _uartTxRx(final int[] txBuff, final int rxLen_, final int timeoutMS) // Tx from the given buffer; Rx into '_txrxBuff'
    {
        // Clear the Rx length
        _rxLastLen = -1;

        // Sanity check
        if(txBuff == null || txBuff.length == 0) return false;
        if(rxLen_ > BufferSize) return false;

        // Calculate the checksum if required
        int cs = txBuff.length;

        if(_uartCS) for(final int v : txBuff) cs = (cs + v) & 0xFF;

        // Send the data
                    _serialTx(txBuff.length);
                    _serialTx(txBuff       );
        if(_uartCS) _serialTx(cs           );

        // Read the response
        boolean res   = true;
        int     rxLen = rxLen_;

        if(timeoutMS > 0) _setSerialRxTimeout_MS(timeoutMS);

        do {

            // Get the length
            final Integer len = _serialRxUInt8();

            if(len == null      ) { res = false; break; }
            if(len >  BufferSize) { res = false; break; }

            if(rxLen > 0) {
                if(len != rxLen) { res = false; break; }
            }
            else {
                rxLen = len;
            }

            // Read the bytes
            if( !_serialRx(_txrxBuff, rxLen) ) { res = false; break; }

            // Check the checksum if required
            if(_uartCS) {
                // Calculate the checksum
                cs = rxLen;
                for(int i = 0; i < rxLen; ++i) cs = (cs + _txrxBuff[i]) & 0xFF;
                // Read and check the checksum
                final Integer rcs = _serialRxUInt8();
                if(len == null) { res = false; break; }
                if(cs != rcs  ) { res = false; break; }
            }

        } while(false);

        if(timeoutMS > 0) _setSerialRxTimeout_MS_default();

        // Save the Rx length
        if(res) _rxLastLen = rxLen;

        // Done
        return res;
    }

    private void _tx(final int[] txBuff) // Tx from the given buffer
    { _uartTx(txBuff); }

    private void _tx(final int txOfs, final int txLen) // Tx from '_txrxBuff' using the specified offset and length
    { _tx( XCom.arrayCopy(_txrxBuff, txOfs, txLen) ); }

    private void _tx(final int txLen) // Tx from '_txrxBuff' starting at offset 0 for the specified length
    { _tx(0, txLen); }

    private boolean _txrx(final int[] txBuff, final int rxLen, final int timeoutMS) // Tx from the given buffer; Rx into '_txrxBuff'
    {
        if( !_uartTxRx(txBuff, rxLen, timeoutMS) ) return false;

        return (_txrxBuff[0] == XCP_CMD_PID_RES); // Error if the 1st value is not XCP_CMD_PID_RES
    }

    private boolean _txrx(final int[] txBuff, final int rxLen) // Tx from the given buffer; Rx into '_txrxBuff'
    { return _txrx(txBuff, rxLen, -1); }

    private boolean _txrx(final int txOfs, final int txLen, final int rxLen, final int timeoutMS) // Tx from '_txrxBuff' using the specified offset and length; Rx into '_txrxBuff'
    { return _txrx( XCom.arrayCopy(_txrxBuff, txOfs, txLen), rxLen, timeoutMS ); }

    private boolean _txrx(final int txLen, final int rxLen, final int timeoutMS) // Tx from '_txrxBuff' starting at offset 0 for the specified length; Rx into '_txrxBuff'
    { return _txrx(0, txLen, rxLen, timeoutMS); }

    private boolean _txrx(final int txLen, final int rxLen) // Tx from '_txrxBuff' starting at offset 0 for the specified length; Rx into '_txrxBuff'
    { return _txrx(0, txLen, rxLen, -1); }

    private void _dumpRxHex(final String prefix) // Dump the last Rx values as hexadecimal
    {
        if(_rxLastLen <= 0) return;

        SysUtil.stdDbg().printf("[%s] ", prefix);

        for(int i = 0; i < _rxLastLen; ++i) SysUtil.stdDbg().printf("%02X ", _txrxBuff[i]);

        SysUtil.stdDbg().println();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _getU16(final int[] buff, final int ofs)
    {
        if(_isLE) {
            return ( ( (buff[ofs + 0] & 0xFF) << 0 ) |
                     ( (buff[ofs + 1] & 0xFF) << 8 )
                   );
        }
        else {
            return ( ( (buff[ofs + 0] & 0xFF) << 8 ) |
                     ( (buff[ofs + 1] & 0xFF) << 0 )
                   );
        }
    }

    private void _putU16(final int[] buff, final int ofs, final int address)
    {
        if(_isLE) {
            buff[ofs + 0] = (address >> 0) & 0xFF;
            buff[ofs + 1] = (address >> 8) & 0xFF;
        }
        else {
            buff[ofs + 0] = (address >> 8) & 0xFF;
            buff[ofs + 1] = (address >> 0) & 0xFF;
        }
    }

    private long _getU32(final int[] buff, final int ofs)
    {
        if(_isLE) {
            return ( ( (buff[ofs + 0] & 0xFFL) <<  0 ) |
                     ( (buff[ofs + 1] & 0xFFL) <<  8 ) |
                     ( (buff[ofs + 2] & 0xFFL) << 16 ) |
                     ( (buff[ofs + 3] & 0xFFL) << 24 )
                   );
        }
        else {
            return ( ( (buff[ofs + 0] & 0xFFL) << 24 ) |
                     ( (buff[ofs + 1] & 0xFFL) << 16 ) |
                     ( (buff[ofs + 2] & 0xFFL) <<  8 ) |
                     ( (buff[ofs + 3] & 0xFFL) <<  0 )
                   );
        }
    }

    private void _putU32(final int[] buff, final int ofs, final long address)
    {
        if(_isLE) {
            buff[ofs + 0] = (int) ( (address >>  0) ) & 0xFF;
            buff[ofs + 1] = (int) ( (address >>  8) ) & 0xFF;
            buff[ofs + 2] = (int) ( (address >> 16) ) & 0xFF;
            buff[ofs + 3] = (int) ( (address >> 24) ) & 0xFF;
        }
        else {
            buff[ofs + 0] = (int) ( (address >> 24) ) & 0xFF;
            buff[ofs + 1] = (int) ( (address >> 16) ) & 0xFF;
            buff[ofs + 2] = (int) ( (address >>  8) ) & 0xFF;
            buff[ofs + 3] = (int) ( (address >>  0) ) & 0xFF;
        }
    }

    private int _getU16(final int ofs)
    { return _getU16(_txrxBuff, ofs); }

    private void _putU16(final int ofs, final int address)
    { _putU16(_txrxBuff, ofs, address); }

    private long _getU32(final int ofs)
    { return _getU32(_txrxBuff, ofs); }

    private void _putU32(final int ofs, final long address)
    { _putU32(_txrxBuff, ofs, address); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _connect()
    {
        // Prepare the command buffer
        final int[] tx = new int[] { XCP_CMD_CONNECT, 0x00 };

        // Try to connect as several times
        for(int r = 0; r < GET_CONNECT_RETRY_COUNT; ++r) {

            // Reset the MCU via DTR/RTS
            _serialResetMCU_DTR_RTS();
            SysUtil.sleepMS(25);

            // Send the command and process the response
            if( _txrx(tx, 8) ) {
                /*
                _dumpRxHex("_connect");
                //*/
                /* Error if the PGM resource is not available
                 *     Bit# 7 6 5 4   3    2   1 0
                 *          X X X PGM STIM DAQ X CAL/PAG
                 */
                 if( (_txrxBuff[1] & XCP_RES_PGM) == 0 ) return false;
                /* Get the endianness
                 *     Bit# 7        6                5 4 3 2                     1                     0
                 *          OPTIONAL SLAVE_BLOCK_MODE X X X ADDRESS_GRANULARITY_1 ADDRESS_GRANULARITY_0 BYTE_ORDER
                 */
                _isLE = (_txrxBuff[2] & 0x01) == 0;
                // Get and check the MAX_CTO
                _maxCTO = _txrxBuff[3];
                if(_maxCTO <= 0 || _maxCTO > BufferSize) return false;
                _maxCTOmin2 = _maxCTO - 2;
                // Get and check the MAX_DTO
                _maxDTO = _getU16(_txrxBuff, 4);
                if(_maxDTO <= 0 || _maxDTO > BufferSize) return false;
                _maxDTOmin1 = _maxDTO - 1;
                // Get the major versions
                _verProto = _txrxBuff[6];
                _verTrans = _txrxBuff[7];
                // Done
                return true;
            }

        } // for

        // Error
        USB2GPIO.notifyError(Texts.ProgXXX_FailConnectBLTout, ProgClassName);
        return false;
    }

    private boolean _getStatus()
    {
        // Prepare the command buffer
        _txrxBuff[0] = XCP_CMD_GET_STATUS;

        // Send the command
        if( !_txrx(1, 6) ) return false;

        /*
        _dumpRxHex("_getStatus");
        //*/

        // Get the current resource protection status
        _curRPP = _txrxBuff[2];

        // Done
        return true;
    }

    private boolean _getSeed()
    {
        // Simply return if execution is not required
        if( (_curRPP & XCP_RES_PGM) == 0 ) return true;

        // Prepare the command buffer
        _txrxBuff[0] = XCP_CMD_GET_SEED;
        _txrxBuff[1] = 0x00;
        _txrxBuff[2] = XCP_RES_PGM;

        // Send the command
        if( !_txrx(3, -1) ) return false;

        /*
        _dumpRxHex("_getSeed");
        //*/

        // Check the number of byte(s)
        final int seedLen = _txrxBuff[1];

        if(seedLen <= 0 || seedLen > _maxCTOmin2) return false;

        // Copy the seed
        final long[] seed = new long[seedLen];

        for(int i = 0; i < seedLen; ++i) seed[i] = _txrxBuff[2 + i];

        // Call the user-defined callback if it exist
        if(_ucb != null) {
            final long[] res = _ucb.handle(UserCallback.REQ_KEY_FROM_SEED, seed);
            if(res == null) return false;
            _key = new int[res.length];
            for(int i = 0; i < res.length; ++i) _key[i] = (int) res[i];
        }

        // Done
        return true;
    }

    private boolean _unlock()
    {
        // Simply return if execution is not required
        if( _key == null || (_curRPP & XCP_RES_PGM) == 0 ) return true;

        // Check if the key is too long
        if(_key.length > _maxCTOmin2) return false;

        // Prepare the command buffer
        int idx = 0;

        _txrxBuff[idx++] = XCP_CMD_UNLOCK;
        _txrxBuff[idx++] = _key.length;

        for(int i = 0; i < _key.length; ++i) _txrxBuff[idx++] = _key[i];

        // Send the command
        if( !_txrx(_key.length + 2, 2) ) return false;

        /*
        _dumpRxHex("_unlock");
        //*/

        // Check the response
        if( (_txrxBuff[1] & XCP_RES_PGM) != 0 ) return false;

        _curRPP &= ~XCP_RES_PGM;

        // Done
        return true;
    }

    private boolean _readInfoTable()
    {
        // Get the information table address and size
        int  itSize = -1;
        long itAddr = -1;

        if(true) {

            // Prepare the command buffer
            _txrxBuff[0] = XCP_CMD_USER_CMD;
            _txrxBuff[1] = XCP_USR_CMD_GET_IT;
            _txrxBuff[2] = XCP_USR_IT_CID_GETINFO;

            // Send the command
            if( !_txrx(1, 8) ) {
                _serialDrain();
                return true; // CMD_GET_IT is not supported
            }

            // Get the size and address
            itSize = _getU16(2);
            itAddr = _getU16(4);

        } // if

        if(itSize <= 0) return true; // Information table is not supported

        // Download the table
        // ##### !!! TODO : What the table is actually for? !!! #####

        // Done
        return true;
    }

    private boolean _disconnect()
    {
        // Send XCP_CMD_PROGRAM_RESET
        if(true) {
            // Prepare the command buffer
            _txrxBuff[0] = XCP_CMD_PROGRAM_RESET;

            // Send the command without checking the result
            _tx(1);
        }

        // Done
        return true;
    }

    private boolean _sendMTA(final long address)
    {
        // Prepare the command buffer
        _txrxBuff[0] = XCP_CMD_SET_MTA;
        _txrxBuff[1] = 0; // Reserved
        _txrxBuff[2] = 0; // ---
        _txrxBuff[3] = 0; // No address extension

        _putU32(4, address);

        // Send the command
        if( !_txrx(8, 1) ) return false;

        // Done
        return true;
    }

    private boolean _clear(final int len)
    {
        // Prepare the command buffer
        _txrxBuff[0] = XCP_CMD_PROGRAM_CLEAR;
        _txrxBuff[1] = 0; // Absolute mode
        _txrxBuff[2] = 0; // Reserved
        _txrxBuff[3] = 0; // ---

        _putU32(4, len);

        // Send the command
        if( !_txrx(8, 1) ) return false;

        // Done
        return true;
    }

    private boolean _upload(final int len) // NOTE : Upload from slave to master (read data)
    {
        // Prepare the command buffer
        _txrxBuff[0] = XCP_CMD_UPLOAD;
        _txrxBuff[1] = len;

        // Send the command
        if( !_txrx(2, -1) ) return false;

        // Check the received length
        if(_rxLastLen != len + 1) return false;

        // Shift the value
        System.arraycopy(_txrxBuff, 1, _txrxBuff, 0, len);

        // Done
        return true;
    }

    private boolean _programStart()
    {
        // Prepare the command buffer
        _txrxBuff[0] = XCP_CMD_PROGRAM_START;

        // Send the command
        if( !_txrx(1, 7) ) return false;

        /*
        _dumpRxHex("_programStart");
        //*/

        // Get and check the MAX_CTO_PGM
        _maxPTO = Math.min(_txrxBuff[3], BufferSize);
        if(_maxPTO <= 0) return false;

        _maxPTOmin1 = _maxPTO - 1;

        // Done
        return true;
    }

    private boolean _programEnd()
    {
        // Prepare the command buffer
        _txrxBuff[0] = XCP_CMD_PROGRAM;
        _txrxBuff[1] = 0;

        // Send the command
        if( !_txrx(2, 1) ) return false;

        /*
        _dumpRxHex("_programEnd");
        //*/

        // Done
        return true;
    }

    private boolean _program(final int[] buff)
    {
        // Prepare the command buffer
        _txrxBuff[0] = XCP_CMD_PROGRAM;
        _txrxBuff[1] = buff.length;

        for(int i = 0; i < buff.length; ++i) _txrxBuff[2 + i] = buff[i];

        // Send the command
        if( !_txrx(buff.length + 2, 1) ) return false;

        /*
        if( !_txrx(buff.length + 2, -1) ) {
            _dumpRxHex("_program");
            return false;
        }
        */

        // Done
        return true;
    }

    private boolean _programMax(final int[] buff)
    {
        // Prepare the command buffer
        _txrxBuff[0] = XCP_CMD_PROGRAM_MAX;

        for(int i = 0; i < buff.length; ++i) _txrxBuff[1 + i] = buff[i];

        // Send the command
        if( !_txrx(buff.length + 1, 1) ) return false;

        /*
        if( !_txrx(buff.length + 1, -1) ) {
            _dumpRxHex("_programMax");
            return false;
        }
        */

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private UserCallback _ucb              = null;
    private boolean      _inProgMode       = false;
    private boolean      _chipErased       = false;

    private long         _flashBaseAddress = -1;
    private int          _bootloaderSize   = -1;
    private int          _userProgVTabSize = -1;

    private boolean      _uartCS           = false;
    private int[]        _key              = null;

    private boolean      _isLE             = false;
    private int          _maxCTO           = -1;
    private int          _maxCTOmin2       = -1;
    private int          _maxPTO           = -1;
    private int          _maxPTOmin1       = -1;
    private int          _maxDTO           = -1;
    private int          _maxDTOmin1       = -1;
    private int          _verProto         = -1;
    private int          _verTrans         = -1;
    private int          _curRPP           = -1;

    public ProgBootOpenBLT(final ProgBootOpenBLT.Config config) throws Exception
    {
        // Process the superclass
        super(ProgClassName, config);

        // Check the configuration values
        if(_config.memoryEEPROM.totalSize > 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMETotSize, ProgClassName);
    }

    @Override
    public byte _flashMemoryEmptyValue()
    { return FlashMemory_EmptyValue; } // ##### !!! TODO : May be different on different MCU !!! #####

    @Override
    public int _flashMemoryAlignWriteSize(final int numBytes)
    { return numBytes; }

    @Override
    public byte _eepromMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    public void setKey(final int key[])
    {
        // NOTE : # This function must be called before 'begin()' if a 'UserCallback' is not specified.
        //        # This function must not be called with a non-null 'key' if a 'UserCallback' is specified.

        _key = (key == null || key.length == 0) ? null : key;
    }

    private boolean _begin_impl(final String serialDevice, final int baudrate, final long flashBaseAddress, final int bootloaderSize, final int userProgramVectorTableSize, final UserCallback ucb)
    {
        final long DEF_FLASH_BASE_ADDRESS = 0x00000000;
        final int  DEF_BOOTLOADER_SIZE    = 0x2000;
        final int  DEF_UPROG_VTAB_SIZE    = 0x0000;

        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Clear flag
        _chipErased = false;

        // Save the user-defined callback
        _ucb = ucb;

        // Save the flash base address
             if(flashBaseAddress >= 0) _flashBaseAddress = flashBaseAddress;
        else if(_ucb == null         ) _flashBaseAddress = DEF_FLASH_BASE_ADDRESS;
        else {
            final long[] res = _ucb.handle(UserCallback.REQ_FLASH_BASE_ADDR, null);
            if(res == null) return false;
            _flashBaseAddress = res[0];
        }

        // Save the bootloader size
             if(bootloaderSize >= 0) _bootloaderSize = bootloaderSize;
        else if(_ucb == null       ) _bootloaderSize = DEF_BOOTLOADER_SIZE;
        else {
            final long[] res = _ucb.handle(UserCallback.REQ_BOOTLOADER_SIZE, null);
            if(res == null) return false;
            _bootloaderSize = (int) res[0];
        }

        // Save the user program vector table size
             if(userProgramVectorTableSize >= 0) _userProgVTabSize = userProgramVectorTableSize;
        else if(_ucb == null                   ) _userProgVTabSize = DEF_UPROG_VTAB_SIZE;
        else {
            final long[] res = _ucb.handle(UserCallback.REQ_UPROG_VTAB_SIZE, null);
            if(res == null) return false;
            _userProgVTabSize = (int) res[0];
        }

        // Open the serial port
        if(baudrate < 0) {
            _uartCS = true;
            _openSerialPort(serialDevice, -baudrate, null);
        }
        else {
            _uartCS = false;
            _openSerialPort(serialDevice,  baudrate, null);
        }

        // Initialize the programmer
        if( !_connect      () ) return false;
        if( !_getStatus    () ) return false;
        if( !_getSeed      () ) return false;
        if( !_unlock       () ) return false;
        if( !_readInfoTable() ) return false;

        // Set flag
        _inProgMode = true;

        // Done
        return true;
    }

    public boolean begin(final String serialDevice, final int baudrate, final long flashBaseAddress, final int bootloaderSize, final int userProgramVectorTableSize, final UserCallback ucb)
    {
        if(ucb != null) {
            if(flashBaseAddress >= 0 || bootloaderSize >= 0 || userProgramVectorTableSize >= 0 || _key != null) return false;
        }

        if( _begin_impl(serialDevice, baudrate, flashBaseAddress, bootloaderSize, userProgramVectorTableSize, ucb) ) return true;

        _closeSerialPort();

        return USB2GPIO.notifyError(Texts.ProgXXX_FailInitPrgDev, ProgClassName);
    }

    public boolean begin(final String serialDevice, final int baudrate, final long flashBaseAddress, final int bootloaderSize, final int userProgramVectorTableSize)
    { return begin(serialDevice, baudrate, flashBaseAddress, bootloaderSize, userProgramVectorTableSize, null); }

    public boolean begin(final String serialDevice, final int baudrate, final UserCallback ucb)
    { return begin(serialDevice, baudrate, -1, -1, -1, ucb); }

    @Override
    public boolean end()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Start the application
        final boolean resDisconnect = _disconnect();

        // Close the serial port
        _closeSerialPort();

        // Clear flag
        _inProgMode = false;

        // Check for error(s)
        if(!resDisconnect) {
            if(!resDisconnect) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPrgDev, ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : All standard bootloader implementations that use the OpenBLT protocol do not support signature reading!

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

    // NOTE : Under the OpenBLT protocol, this function is expected to erase only flash memory
    @Override
    public boolean chipErase()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_chipErased) return true;

        // Start programming
        if( !_programStart () ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        // Erase the chip
        final long eraseAddr  =  _flashBaseAddress            + _bootloaderSize;
              int  eraseTotal = _config.memoryFlash.totalSize - _bootloaderSize;
        final int  eraseSize  = _config.memoryFlash.pageSize;
        final int  numPages   = eraseTotal / eraseSize;

        for(int i = 0; i < numPages; ++i) {

            if( !_sendMTA(eraseAddr + eraseSize * i) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            if( !_clear( Math.min(eraseSize, eraseTotal) ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            eraseTotal -= eraseSize;

        } // for

        // End programming
        if( !_programEnd () ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

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
              long sa = (startAddress < 0) ? _flashBaseAddress             : startAddress;
        final int  nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        if(sa >= _flashBaseAddress) sa -= _flashBaseAddress;

        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_even( (int) sa, nb, _config.memoryFlash.totalSize, ProgClassName ) ) return -1;

        // Prepare the result buffer
        if(_config.memoryFlash.readDataBuff == null || _config.memoryFlash.readDataBuff.length != numBytes) {
            _config.memoryFlash.readDataBuff = new int[numBytes];
        }

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Index counter
        int rdbIdx = 0;
        int verIdx = 0;

        // Determine the number of chunks
        final int     ChunkSize  = Math.min(_config.memoryFlash.pageSize, _maxDTOmin1 / 2 * 2);

        final boolean notAligned = (numBytes % ChunkSize) != 0;
        final int     numChunks  = (numBytes / ChunkSize) + (notAligned ? 1 : 0);

        // Read the bytes (and compare them if requested)
        /*
        int[] checksum = null;
        */

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, numBytes - rdbIdx);

            if( !_sendMTA(_flashBaseAddress + sa + c * ChunkSize) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                return -1;
            }

            if( !_upload(numReads) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                return -1;
            }

            /*
            if(0x210C >= (sa + c * ChunkSize) && 0x210C <= (sa + c * ChunkSize) + ChunkSize) {
                for(int k = 0; k < 128; ++k) { if(k >= 0x0C && k < 0x0C + 4) SysUtil.stdDbg().printf( "%02X ", refData[verIdx + k] ); }
                SysUtil.stdDbg().println();
                for(int k = 0; k < 128; ++k) { if(k >= 0x0C && k < 0x0C + 4) SysUtil.stdDbg().printf( "%02X ", _txrxBuff[k]        ); }
                SysUtil.stdDbg().println();
            }
            //*/

            // Process the chunk bytes
            for(int b = 0; b < numReads; b += 2) {

                // Store the bytes to the result buffer
                _config.memoryFlash.readDataBuff[rdbIdx++] = _txrxBuff[b    ];
                _config.memoryFlash.readDataBuff[rdbIdx++] = _txrxBuff[b + 1];

                // Check if it is in the checksum area
                final boolean inCSArea = (verIdx >= _userProgVTabSize && verIdx < _userProgVTabSize + 4);

                // Compare the bytes as needed
                if(refData != null && verIdx < refData.length) {
                    if(inCSArea) {
                        /*
                        // Calculate the checksum as needed
                        if(checksum == null) {
                            /*
                             * Layout of the vector table for exception addresses for most ARM MCUs:
                             *    0x......00 Initial stack pointer
                             *    0x......04 Reset Handler
                             *    0x......08 NMI Handler
                             *    0x......0C Hard Fault Handler
                             *    0x......10 MPU Fault Handler
                             *    0x......14 Bus Fault Handler
                             *    0x......18 Usage Fault Handler
                             *
                             * UInt32 Checksum  = SUM( [0x00] ... [0x18] )
                             *        Checksum  = ~Checksum
                             *        Checksum += 1
                             * /
                             // ##### !!! TODO : Not all MCU have 7 entries !!! #####
                             // ##### ??? TODO : Add 'userProgramVectorTableEntryCount' and 'REQ_UPROG_VTE_COUNT' ??? #####
                             long cs = 0;
                             for(int i = 0; i <= 0x18; i += 0x04) {
                                 final long value = ( (long) _config.memoryFlash.readDataBuff[i + 0] <<  0 )
                                                  | ( (long) _config.memoryFlash.readDataBuff[i + 1] <<  8 )
                                                  | ( (long) _config.memoryFlash.readDataBuff[i + 2] << 16 )
                                                  | ( (long) _config.memoryFlash.readDataBuff[i + 3] << 24 );
                                 cs = (cs + value) & 0xFFFFFFFFL;
                             }
                             cs  = ~(cs) & 0xFFFFFFFFL;
                             cs += 1;
                             // Store the checksum
                             checksum = new int[] {
                                            (int) ( (cs >>  0) & 0xFF ),
                                            (int) ( (cs >>  8) & 0xFF ),
                                            (int) ( (cs >> 16) & 0xFF ),
                                            (int) ( (cs >> 24) & 0xFF )
                                        };
                        }
                        // Check the checksum
                        // ##### ??? TODO : Patch 'refData' so it contains the checksum bytes ??? #####
                        final int csIdx = verIdx - _userProgVTabSize;
                        if( _config.memoryFlash.readDataBuff[verIdx] != checksum[csIdx + 0] ) return verIdx;
                        ++verIdx;
                        if( _config.memoryFlash.readDataBuff[verIdx] != checksum[csIdx + 1] ) return verIdx;
                        ++verIdx;
                        */
                        // Ignore the checksum bytes
                        // NOTE : OpenBLT checksum calculations are not always consistent across all MCUs,
                        //        so for now, ignore the checksum
                        ++verIdx;
                        ++verIdx;
                    }
                    else {
                        // Check the firmware
                        if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                        ++verIdx;
                        if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                        ++verIdx;
                    }
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

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _writeFlash_impl(final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Get the number of chunks to be written and the current address
        final int  ChunkSize = Math.min(_config.memoryFlash.pageSize, _maxPTOmin1 / 2 * 2);

        final int  numChunks = nb / ChunkSize;
              long cpgAddr   = sa;

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Start programming
        if( !_programStart () ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        // Clear flag
        _chipErased = false;

        // Write the chunks
        int datIdx = 0;

        for(int p = 0; p < numChunks; ++p) {

            // ##### !!! TODO : Skip writing the chunk if it is blank (its contents are all 'FlashMemory_EmptyValue') !!! #####

            // Write the chunk
            if( !_sendMTA(_flashBaseAddress + cpgAddr) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            if(ChunkSize < _maxPTOmin1) {
                if( !_program( USB2GPIO.ba2ia(data, datIdx, ChunkSize) ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
            }
            else {
                if( !_programMax( USB2GPIO.ba2ia(data, datIdx, ChunkSize) ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
            }

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, ChunkSize / 2);

            // Increment the counters
            cpgAddr += ChunkSize;
            datIdx  += ChunkSize;

        } // for p

        // End programming
        if( !_programEnd () ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

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
              long sa = (startAddress < 0) ? _flashBaseAddress             : startAddress;
        final int  nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        if(sa >= _flashBaseAddress) sa -= _flashBaseAddress;

        // Align the number of bytes and pad the buffer as needed
        final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer( data, (int) sa, nb, _config.memoryFlash.pageSize, _config.memoryFlash.totalSize, FlashMemory_EmptyValue, ProgClassName );
        if(anbr == null) return false;

        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_pageSize( (int) sa, anbr.nb, _config.memoryFlash.pageSize, ProgClassName ) ) return false;

        // Write the flash
        return _writeFlash_impl(anbr.buff, (int) sa, anbr.nb, progressCallback);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : All standard bootloader implementations that use the OpenBLT protocol do not support EEPROM reading and writing!

    @Override
    public int readEEPROM(final int address)
    { return -1; }

    @Override
    public boolean writeEEPROM(final int address, final byte data)
    { return false; }

} // class ProgBootOpenBLT
