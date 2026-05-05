/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;

import java.time.LocalDateTime;

import java.util.Arrays;
import java.util.function.IntConsumer;

import jxm.*;
import jxm.xb.*;


/*
 * Simple implementation of the URCLOCK protocol for programming MCUs with compatible bootloaders
 * (e.g., 'urclock'). Limitations:
 *     1. Does not support backward compatibility mode.
 *     2. Provides only limited metadata support.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written partially based on the algorithms and information found from:
 *
 *     AVR061: STK500 Communication Protocol
 *     https://www.microchip.com/content/dam/mchp/documents/OTH/ApplicationNotes/ApplicationNotes/doc2525.pdf
 *
 *     Urprotocol
 *     https://github.com/stefanrueger/urboot/blob/main/urprotocol.md
 *
 *     Urboot - Feature-rich small AVR bootloader using urprotocol
 *     https://github.com/stefanrueger/urboot
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * JxMake reimplements protocol behavior based on available documentation, supplemented by runtime
 * observation and bench-level validation where features are undocumented or ambiguous. No URBOOT
 * source code or expressive implementation logic is used. Constants and timing are derived from
 * functional analysis and do not constitute derivative work.
 */
public class ProgBootURCLOCK extends ProgBootSerial {

    private static final String ProgClassName = "ProgBootURCLOCK";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class Config extends ProgBootSerial.Config {

        public static class MCUInfo implements Serializable {
            public int numVectors = -1;
            public int totalSRAM  = -1;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final MCUInfo mcuInfo = new MCUInfo();

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'xxx*()' functions ??? #####

// ##### ??? TODO : How to combine this with 'BootAVRSimpleUART.java.inc' ??? #####


public static Config ATmega8A()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

    config.mcuInfo.numVectors     =   19;
    config.mcuInfo.totalSRAM      = 1024;

    return config;
}

public static Config ATmega16A()
{
    final Config config = ATmega8A();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;

    config.mcuInfo.numVectors     =    21;

    return config;
}

public static Config ATmega32A()
{
    final Config config = ATmega16A();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    config.mcuInfo.numVectors     =    26;
    config.mcuInfo.totalSRAM      =  2048;

    return config;
}

public static Config ATmega64A()
{
    final Config config = ATmega32A();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;

    config.memoryEEPROM.totalSize =  2048;

    config.mcuInfo.numVectors     =    35;
    config.mcuInfo.totalSRAM      =  4096;

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

    config.mcuInfo.numVectors     =   26;
    config.mcuInfo.totalSRAM      =  512;

    return config;
}

public static Config ATmega88P()
{
    final Config config = ATmega48P();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

    config.mcuInfo.totalSRAM      = 1024;

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

    config.mcuInfo.totalSRAM      =  2048;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega164P()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;
    config.memoryFlash.numPages   =   128;

    config.memoryEEPROM.totalSize =   512;

    config.mcuInfo.numVectors     =    35;
    config.mcuInfo.totalSRAM      =  1024;

    return config;
}

public static Config ATmega324P()
{
    final Config config = ATmega164P();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    config.mcuInfo.totalSRAM      =  2048;

    return config;
}

public static Config ATmega644P()
{
    final Config config = ATmega324P();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;

    config.memoryEEPROM.totalSize =  2048;

    config.mcuInfo.totalSRAM      =  4096;

    return config;
}

public static Config ATmega1284P()
{
    final Config config = ATmega644P();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    config.memoryEEPROM.totalSize =   4096;

    config.mcuInfo.totalSRAM      =  16384;

    return config;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    private Config.MCUInfo _configMCUInfo()
    { return ( (Config) super._config ).mcuInfo; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // STK500 responses
    private static final int RES_STK_OK                 = 0x10; // DLE - data link escape
    private static final int RES_STK_INSYNC             = 0x14; // DC4 - device control 4

    // STK500 commands
    private static final int SYN_CRC_EOP                = 0x20; // ' '

    private static final int CMD_STK_GET_SYNC           = 0x30; // '0'

    private static final int CMD_STK_ENTER_PROGMODE     = 0x50; // 'P'
    private static final int CMD_STK_LEAVE_PROGMODE     = 0x51; // 'Q'
    private static final int CMD_STK_CHIP_ERASE         = 0x52; // 'R'

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Features specific to the target's URBOOT variant
    private              boolean _UB_ReadFlash          = false;
    private              boolean _UB_FlashLLNOR         = false; // ##### !!! TODO : What to do if this flag is 'true'? !!! #####
    private              boolean _UB_ChipErase          = false;

    private              int     _UB_VerMajor           = -1;
    private              int     _UB_VerMinor           = -1;

    private              boolean _UB_EEPROMSupport      = false;

    private              boolean _UB_IsVBL              = false;
    private              boolean _UB_VBLSelfPatch       = false;
    private              boolean _UB_VBLSelfPatchVerify = false;

    private              int     _UB_BLStart            = -1;
    private              int     _UB_BLEnd              = -1;
    private              int     _UB_AppStart           = -1;
    private              int     _UB_AppEnd             = -1;
    private              int     _UB_AppJVNum           = -1;
    private              int     _UB_JxMakeMetaAddr     = -1;

    // URCLOCK responses (depends on the target's URBOOT variant)
    private              int     _UR_OK                 = -1;
    private              int     _UR_INSYNC             = -1;

    // URCLOCK commands
    private static final int     CMD_UR_PROG_PAGE_EE    = 0x00; // NC  - null character
    private static final int     CMD_UR_READ_PAGE_EE    = 0x01; // SH  - start of heading

    private static final int     CMD_UR_PROG_PAGE_FL    = 0x02; // ST  - start of text
    private static final int     CMD_UR_READ_PAGE_FL    = 0x03; // ED  - end of text

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int GET_SYNC_RETRY_COUNT = 32;
    private static final int GET_SYNC_RESET_SKIP  =  4;

    private void _urResetData()
    {
        _UB_ReadFlash          = false;
        _UB_FlashLLNOR         = false;
        _UB_ChipErase          = false;

        _UB_VerMajor           = -1;
        _UB_VerMinor           = -1;

        _UB_EEPROMSupport      = false;

        _UB_IsVBL              = false;
        _UB_VBLSelfPatch       = false;
        _UB_VBLSelfPatchVerify = false;

        _UB_BLStart            = -1;
        _UB_BLEnd              = -1;
        _UB_AppStart           = -1;
        _UB_AppEnd             = -1;
        _UB_AppJVNum           = -1;
        _UB_JxMakeMetaAddr     = -1;

        _UR_OK                 = -1;
        _UR_INSYNC             = -1;

        _eepromBuffer          = null;
        _eepromFDirty          = null;
    }

    private boolean _urConnect()
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
            // Required for autobaud detection in URBOOT variants that support it
            _serialTx(_pbs_wrBuff_32I, 2);
            _serialDrain();
            // Send the command and get the response
            _serialTx(_pbs_wrBuff_32I, 2);
            final Integer ur_isync = _serialRxUInt8();
            final Integer ur_ok    = _serialRxUInt8();
            if(ur_isync != null && ur_ok != null) {
                if( (ur_isync != ur_ok) && (ur_isync != RES_STK_INSYNC || ur_ok != RES_STK_OK) ) {
                    // Copy the constants
                    _UR_OK     = ur_ok;
                    _UR_INSYNC = ur_isync;
                    /*
                    SysUtil.stdDbg().printf("%d _UR_OK=%02X ; _UR_INSYNC=%02X\n", i, _UR_OK, _UR_INSYNC);
                    //*/
                    // Get the bootloader information
                    final int bootinfo = ur_isync * 255 + ur_ok;
                    final int mcuid    = bootinfo % 2040;
                    final int features = bootinfo / 2040;
                    _UB_ReadFlash  = (features & 0x04) != 0;
                    _UB_FlashLLNOR = (features & 0x08) != 0;
                    _UB_ChipErase  = (features & 0x10) != 0;
                    /*
                    SysUtil.stdDbg().printf("MCUID=%3d ; _UB_ReadFlash=%b ; _UB_FlashLLNOR=%b ; _UB_ChipErase=%b\n", mcuid, _UB_ReadFlash, _UB_FlashLLNOR, _UB_ChipErase);
                    //*/
                    // Save the MCU ID as the signature, since URCLOCK does not support reading the actual MCU signature bytes
                    if(mcuid >= 0) _mcuSignature = new int[] { mcuid };
                    // Done
                    _serialDrain();
                    return true;
                }
            }
        }

        // Not done
        return false;
    }

    private boolean _rxChk(final int ignoreCnt, final int recvBuff[], final int recvLen)
    {
        // Check for UR_INSYNC
        if( !_serialRxChkUInt8(_UR_INSYNC) ) return USB2GPIO.notifyError(
            Texts.ProgXXX_FailBLPrgCmdXErrI(Texts.CmdXErr_BLInvalidValue, "UR_INSYNC"), ProgClassName
        );

        // Ignore the number of requested bytes
        if(ignoreCnt > 0) {
            if( !_serialRx(new byte[ignoreCnt], ignoreCnt) ) return false;
        }

        // Read and store the number of requested bytes
        if(recvBuff != null) {
            if( !_serialRx(recvBuff, recvLen) ) return false;
        }

        // Check for UR_OK
        if( !_serialRxChkUInt8(_UR_OK) ) return USB2GPIO.notifyError(
            Texts.ProgXXX_FailBLPrgCmdXErrI(Texts.CmdXErr_BLInvalidValue, "UR_OK"), ProgClassName
        );

        // Done
        return true;
    }

    private boolean _execCmd(final int[] buff, final int cmdLen)
    {
        // Send the command
        _serialTx(buff, cmdLen);

        // Check and return the response
        return _rxChk(0, null, 0);
    }

    private boolean _execCmd(final int[] buff, final int cmdLen, final int recvBuff[], final int recvLen)
    {
        // Send the command
        _serialTx(buff, cmdLen);

        // Check and return the response
        return _rxChk(0, recvBuff, recvLen);
    }

    private boolean _execCmd(final int[] buff, final int cmdLen, final int recvBuff[])
    { return _execCmd(buff, cmdLen, recvBuff,recvBuff.length); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _getSync()
    {
        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_GET_SYNC;
        _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

        // Executes the command and check its success or failure state
        if( !_execCmd(_pbs_wrBuff_32I, 2) ) return false;

        // Done
        _serialDrain();
        return true;
    }

    private boolean _enterProgMode()
    {
        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_ENTER_PROGMODE;
        _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

        // Executes the command and returns its success or failure state
        return _execCmd(_pbs_wrBuff_32I, 2);
    }

    private boolean _leaveProgMode()
    {
        // Prepare the command
        _pbs_wrBuff_32I[0] = CMD_STK_LEAVE_PROGMODE;
        _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

        // Executes the command and returns its success or failure state
        return _execCmd(_pbs_wrBuff_32I, 2);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _prepXXX_impl(final int cmd, final int len, final int address, final boolean eop)
    {
        // Prepare the command
        final boolean lfz = ( _config.memoryFlash.totalSize > (64 * 1024) );
        final boolean lpz = ( _config.memoryFlash.pageSize  > 256         );
              int     idx = 0;

                    _pbs_wrBuff_32I[idx++] = cmd;
                    _pbs_wrBuff_32I[idx++] = (address >>  0) & 0xFF;
                    _pbs_wrBuff_32I[idx++] = (address >>  8) & 0xFF;
        if( lfz)    _pbs_wrBuff_32I[idx++] = (address >> 16) & 0xFF; // Extended address for flash larger than 64kB
        if(!lpz)    _pbs_wrBuff_32I[idx++] = len;
        else     {  _pbs_wrBuff_32I[idx++] = (len >> 8) & 0xFF;
                    _pbs_wrBuff_32I[idx++] = (len >> 0) & 0xFF; }
        if( eop)    _pbs_wrBuff_32I[idx++] = SYN_CRC_EOP;

        // Returns the number of bytes in the command
        return idx;
    }

    private boolean _readXXX_impl(final int cmd, final int[] buff, final int len, final int address)
    {
        // Sanity check
        if(!_UB_ReadFlash) return false;

        if(len > buff.length || len > _config.memoryFlash.pageSize) return false;

        // Prepare the command
        final int cmdLen = _prepXXX_impl(cmd, len, address, true);

        // Prepare the command
        final boolean lfz = ( _config.memoryFlash.totalSize > (64 * 1024) );
        final boolean lpz = ( _config.memoryFlash.pageSize  > 256         );
              int     idx = 0;

                    _pbs_wrBuff_32I[idx++] = cmd;
                    _pbs_wrBuff_32I[idx++] = (address >>  0) & 0xFF;
                    _pbs_wrBuff_32I[idx++] = (address >>  8) & 0xFF;
        if( lfz)    _pbs_wrBuff_32I[idx++] = (address >> 16) & 0xFF; // Extended address for flash larger than 64kB
        if(!lpz)    _pbs_wrBuff_32I[idx++] = len;
        else     {  _pbs_wrBuff_32I[idx++] = (len >> 8) & 0xFF;
                    _pbs_wrBuff_32I[idx++] = (len >> 0) & 0xFF; }
                    _pbs_wrBuff_32I[idx++] = SYN_CRC_EOP;

        // Executes the command and returns its success or failure state
        return _execCmd(_pbs_wrBuff_32I, cmdLen, buff, len);
    }

    private boolean _readFlash(final int[] buff, final int len, final int address)
    { return _readXXX_impl(CMD_UR_READ_PAGE_FL, buff, len, address); }

    private int _readFlashMulti(final int[] buff, final int len, final int address)
    {
        // Determine the number of chunks
        final int     ChunkSize  = _config.memoryFlash.pageSize;

        final boolean notAligned = (len % ChunkSize) != 0;
        final int     numChunks  = (len / ChunkSize) + (notAligned ? 1 : 0);

        // Read the bytes (and compare them if requested)
        final int[] cbytes = new int[ChunkSize];
              int   rdbIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, len - rdbIdx);

            if( !_readFlash(cbytes, ChunkSize, address + c * ChunkSize) ) return -1;

            // Store the bytes to the result buffer
            for(int b = 0; b < numReads; ++b) buff[rdbIdx++] = cbytes[b];

        } // for c

        // Done
        return len;
    }

    private boolean _readEEPROM(final int[] buff, final int len, final int address)
    { return !_UB_EEPROMSupport ? false : _readXXX_impl(CMD_UR_READ_PAGE_EE, buff, len, address); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _writeXXX_impl(final int cmd, final int[] buff, final int address)
    {
        // Prepare the command
        final int cmdLen = _prepXXX_impl(cmd, buff.length, address, false);

        // Send the command
        _serialTx(_pbs_wrBuff_32I, cmdLen);

        // Send the data
        _serialTx(buff);

        // Send SYN_CRC_EOP
        _serialTx(SYN_CRC_EOP);

        // Returns its success or failure state
        return _rxChk(0, null, 0);
    }

    private boolean _writeFlash(final int[] buff, final int address)
    {
        // Sanity check
        if(buff.length != _config.memoryFlash.pageSize) return false;

        // Write the flash
        return _writeXXX_impl(CMD_UR_PROG_PAGE_FL, buff, address);
    }

    private boolean _writeEEPROM(final int[] buff, final int address)
    {
        // Sanity check
        if(!_UB_EEPROMSupport) return false;

        if(buff.length > _config.memoryFlash.pageSize) return false;

        // Write the EEPROM
        return  _writeXXX_impl(CMD_UR_PROG_PAGE_EE, buff, address);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int _toU16(final int buff[], final int ofs)
    { return  buff[ofs + 0] | (buff[ofs + 1] << 8); }

    private static long _toU32(final int buff[], final int ofs)
    { return ( (long) ( buff[ofs + 0] | (buff[ofs + 1] << 8) | (buff[ofs + 2] << 16) | (buff[ofs + 3] << 24) ) ) & 0xFFFFFFFFL; }

    private static void _putU16(final int buff[], final int ofs, final int val)
    {
        buff[ofs + 0] = (val >> 0) & 0xFF;
        buff[ofs + 1] = (val >> 8) & 0xFF;
    }

    private static void _putU32(final int buff[], final int ofs, final long val)
    {
        buff[ofs + 0] = (int) ( (val >>  0) & 0xFF );
        buff[ofs + 1] = (int) ( (val >>  8) & 0xFF );
        buff[ofs + 2] = (int) ( (val >> 16) & 0xFF );
        buff[ofs + 3] = (int) ( (val >> 24) & 0xFF );
    }

    private boolean _isFlashPageAligned(final int n)
    { return ( n & (_config.memoryFlash.pageSize - 1) ) == 0; }

    private boolean _hasGT8kFlash()
    { return _config.memoryFlash.totalSize > ( 8 * 1024); }

    private boolean _hasGT64kFlash()
    { return _config.memoryFlash.totalSize > (64 * 1024); }

    private boolean _hasPowerOfTwoFlash()
    { return ( _config.memoryFlash.totalSize & (_config.memoryFlash.totalSize - 1) ) == 0; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean _avr_opcode_is_Inst32(final int opcode)
    {
        final int feof = opcode & 0xFE0F;
        final int feoe = opcode & 0xFE0E;

        return (
            (feof == 0x9200) || // STS  : 1001 001d dddd 0000 kkkk kkkk kkkk kkkk
            (feof == 0x9000) || // LDS  : 1001 000d dddd 0000 kkkk kkkk kkkk kkkk
            (feoe == 0x940C) || // JMP  : 1001 010k kkkk 110k kkkk kkkk kkkk kkkk
            (feoe == 0x940E)    // CALL : 1001 010k kkkk 111k kkkk kkkk kkkk kkkk
        );
    }

    private static boolean _avr_opcode_is_RET(final int opcode)
    { return opcode == 0x9508; }

    private static boolean _avr_opcode_is_RJMP(final int opcode)
    { return (opcode & 0xF000) == 0xC000; }

    private static boolean _avr_opcode_is_JMP(final int opcode)
    { return (opcode & 0xFE0E) == 0x940C; }

    private int _avr_RJMP_ofsWrap(final int s12_ofs)
    {
        final int size = Math.min(_config.memoryFlash.totalSize, 8192);

        int wofs = s12_ofs & (size - 1);
        if(wofs >= size / 2) wofs -= size;

        return wofs;
    }

    private int _avr_RJMP_ofsGet(final int rjmpOpcode)
    {
        // RJMP -> PC = PC + k + 1
        // 1100 kkkk kkkk kkkk

        int ofs;

        ofs = rjmpOpcode & 0x0FFF;          // Extract the signed 12-bit word offset
        ofs = ( (ofs << 4) & 0xFFFF ) >> 3; // Sign-extend and convert to byte address

        return _avr_RJMP_ofsWrap(ofs + 2);  // +1
    }

    private int _avr_opcode_gen_RJMP(final int s12_ofs)
    {
        // RJMP -> PC = PC + k + 1
        // 1100 kkkk kkkk kkkk

        int ofs = _avr_RJMP_ofsWrap(s12_ofs) >> 1; // Convert to word address

        return 0xC000 | ( (ofs - 1) & 0x0FFF );

        // NOTE : 0xCFFF is an endless loop because PC = PC + (-1) + 1 = PC
        //        0xC000 is a  no-operation because PC = PC + ( 0) + 1 = PC + 1
    }

    private int _avr_opcode_gen_RJMP_toBLStart(final int blStart)
    { return _avr_opcode_gen_RJMP(blStart - _config.memoryFlash.totalSize); }

    private static int _avr_JMP_addrGet(final long jmpOpcode)
    {
        // JMP -> PC = k
        // 1001 010k kkkk 110k kkkk kkkk kkkk kkkk -> kkkk kkkk kkkk kkkk 1001 010k kkkk 110k as 'long'

        long addr  = (  jmpOpcode           >> 16 ); // Extract lower 16 bits of the jump address from the upper half of the opcode
             addr |= ( (jmpOpcode & 0x0001) << 16 ); // Add bit  16    from the lower half of the opcode
             addr |= ( (jmpOpcode & 0x01F0) << 13 ); // Add bits 17-21 from the lower half of the opcode

        return (int) (addr << 1); // Return as byte address
    }

    private static long _avr_opcode_gen_JMP(final int u22_adr)
    {
        // JMP -> PC = k
        // 1001 010k kkkk 110k kkkk kkkk kkkk kkkk -> kkkk kkkk kkkk kkkk 1001 010k kkkk 110k as 'long'

        final long addr = u22_adr >> 1;  // Convert to word address

        final long lo   = ( 0x940C                         )  // Base opcode for JMP
                        | ( ( (addr >> 16) & 0x0001 )      )  // Bit  16    -> bit  0
                        | ( ( (addr >> 17) & 0x001F ) << 4 ); // Bits 17-21 -> bits 4-8

        final long hi   = addr & 0xFFFF;                      // Bits 0-15

        return (hi << 16) | lo; // Pack into 32-bit JMP instruction
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * AVRDUDE uses the following metadata layout, placed just below the bootloader:
     *     # Optional filename/description (up to 254 bytes, including null terminator)
     *     # Optional upload date, encoded in this order:
     *           + 2-byte year   [1, 2999], little endian
     *           + 1-byte month  [1,   12]
     *           + 1-byte day    [1,   31]
     *           + 1-byte hour   [0,   23]
     *           + 1-byte minute [0,   59]
     *     # Optional program store description, consisting of:
     *           + Start address in flash (which is the size of the program), encoded as:
     *                 - 2 bytes little-endian number for ≤ 64kB flash
     *                 - 4 bytes little-endian number for > 64kB flash
     *           + Size (distance between program and metadata), encoded similarly
     *     # A single byte called 'mcode' (just below the bootloader), encodes how much metadata is present:
     *           + 255   : no metadata; no store used
     *           + 2-254 : full metadata present; value is the length of filename/description (including null)
     *           + 1     : upload date and program store description; no filename/description
     *           + 0     : only program store description present
     */

    private boolean _dumpMetadata()
    {
        // Calculate the maximum possible size of the metadata in bytes and allocate the buffer
        final int   metaSize = 254 + 6 + ( _hasGT64kFlash() ? 8 : 4 ) + 1;
        final int[] metaBuff = new int[metaSize];

        // Read the metadata
        if( _readFlashMulti(metaBuff, metaSize, _UB_AppEnd + 1 - metaSize ) != metaSize ) return false;

        // Get and check the MCODE
              int metaIdx = metaSize - 1;
        final int mcode   = metaBuff[metaIdx--];

        if(mcode == 255) return true; // No metadata

        // Print the opening text
        SysUtil.stdDbg().printf(Texts.ProgXXX_InfoBLMetadataBeg, ProgClassName);

        // Print the address and MCODE
        final int    nameLen  = 11;
        final int    metaAddr = _UB_AppEnd + 1 - (metaSize + mcode - 254);
        final String fmtAddr  = _hasGT64kFlash() ? "0x%06X (%8d)" : "0x%04X (%5d)";

            SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific(nameLen, "MCODE"                  , "%d"                    ), mcode                );
            SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific(nameLen, Texts.CmdXInf_BLAddress  , fmtAddr                 ), metaAddr, metaAddr   );

        // Get and print the program store description
        if(mcode >= 0) {
            int start = -1;
            int size  = -1;
            if( _hasGT64kFlash() ) {
                start = (int) _toU32(metaBuff, metaIdx - 7);
                size  = (int) _toU32(metaBuff, metaIdx - 3);
                metaIdx -= 8;
            }
            else {
                start = _toU16(metaBuff, metaIdx - 3);
                size  = _toU16(metaBuff, metaIdx - 1);
                metaIdx -= 4;
            }
            SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific(nameLen, Texts.CmdXInf_BLPStrStart, fmtAddr                 ), start, start         );
            SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific(nameLen, Texts.CmdXInf_BLPStrSize , fmtAddr                 ), size , size          );
        }

        // Get and print the upload date
        if(mcode >= 1) {
            final int mm   =  metaBuff[metaIdx--];
            final int hh   =  metaBuff[metaIdx--];
            final int DD   =  metaBuff[metaIdx--];
            final int MM   =  metaBuff[metaIdx--];
            final int YYYY = (metaBuff[metaIdx--] << 8) | metaBuff[metaIdx--];
            SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific(nameLen, Texts.CmdXInf_BLUplDate  , Texts._fmt_YYYYMMDD_hhmm), YYYY, MM, DD, hh, mm );
        }

        // Get and print the filename or description
        if(mcode >= 2) {
            final char[] fnds = new char[mcode];
            for(int i = 0; i < mcode; ++i) fnds[mcode - 1 - i] = (char) metaBuff[metaIdx--];
            SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMSpecific(nameLen, Texts.CmdXInf_BLDesc     , "%s"                    ), String.valueOf(fnds) );
        }

        // Print the closing text
        SysUtil.stdDbg().println(Texts.ProgXXX_InfoBLMetadataEnd);

        // Done
        return true;
    }

    private static final char[] JXMAKE_URBOOT_META_DESC = "Programmed by JxMake\0".toCharArray();

    private int _jxmakeMetadataSize()
    { return  JXMAKE_URBOOT_META_DESC.length + 6 + ( _hasGT64kFlash() ? 8 : 4 ) + 1; }

    private int[] _jxmakeMetadataGet(final int progSize)
    {
        // Prepare the buffer
        final int[] buff = new int[ _jxmakeMetadataSize() ];
              int   idx  = 0;

        // Put the description
        for(final char c : JXMAKE_URBOOT_META_DESC) buff[idx++] = c & 0xFF;

        // Put the upload date
        final LocalDateTime now  = LocalDateTime.now();

        final int           YYYY = now.getYear      ();
        final int           MM   = now.getMonthValue();
        final int           DD   = now.getDayOfMonth();
        final int           hh   = now.getHour      ();
        final int           mm   = now.getMinute    ();

        buff[idx++] = (YYYY >> 0) & 0xFF;
        buff[idx++] = (YYYY >> 8) & 0xFF;
        buff[idx++] = MM;
        buff[idx++] = DD;
        buff[idx++] = hh;
        buff[idx++] = mm;

        // Put the program store description
        final int gap = _UB_AppEnd + 1 - progSize - buff.length;

        if( _hasGT64kFlash() ) {
            _putU32(buff, idx, progSize); idx += 4;
            _putU32(buff, idx, gap     ); idx += 4;
        }
        else {
            _putU16(buff, idx, progSize); idx += 2;
            _putU16(buff, idx, gap     ); idx += 2;
        }

        // Put the MCODE
        buff[idx++] = JXMAKE_URBOOT_META_DESC.length;

        // Return the padded buffer
        final int[] padded    = new int[_config.memoryFlash.pageSize];
        final int   padLength = padded.length - buff.length;

        Arrays.fill(padded, 0, padLength, FlashMemory_EmptyValue);

        System.arraycopy(buff, 0, padded, padLength, buff.length);

        return padded;
    }

    private boolean _readParseTable()
    {
        // Read the last 6 bytes on the flash
        final int[] buff = new int[6];

        if( !_readFlash(buff, buff.length, _config.memoryFlash.totalSize - buff.length) ) return false;

        // Get the version number
        _UB_VerMajor = buff[5] >> 3;
        _UB_VerMinor = buff[5] &  0b00000111;

        // Get the capabilities that this class cares about
        _UB_EEPROMSupport      = (buff[4] & 0b01000000) != 0;

        _UB_IsVBL              = (buff[4] & 0b00001100) != 0;
        _UB_VBLSelfPatch       = (buff[4] & 0b00001000) == 0b00001000;
        _UB_VBLSelfPatchVerify = (buff[4] & 0b00001100) == 0b00001100;

        /*
        SysUtil.stdDbg().printf("_UB_EEPROMSupport=%b ; _UB_IsVBL=%b ; _UB_VBLSelfPatch=%b ; _UB_VBLSelfPatchVerify=%b\n", _UB_EEPROMSupport, _UB_IsVBL, _UB_VBLSelfPatch, _UB_VBLSelfPatchVerify);
        //*/

        // Get the most likely 'RJMP' instruction
        final int rjmp = _toU16(buff, 2);

        // Get the vector number for the '[R]JMP' to the application if it is a vector bootloader
        _UB_AppJVNum = buff[1];

        if(_UB_IsVBL && _UB_AppJVNum <= 0) return USB2GPIO.notifyError(
            Texts.ProgXXX_FailBLResultXErrI(Texts.CmdXErr_BLInvalidValue, "UR_BLVectorNum"), ProgClassName
        );

        // Get the booloader size
        final int blSize = buff[0] * _config.memoryFlash.pageSize;

        // Calculate the bootloader start address, bootloader end address, and application end address
        final int ofsFromEnd = _avr_RJMP_ofsGet(rjmp) - 4;

        if( _avr_opcode_is_RET(rjmp) || (ofsFromEnd >= -blSize && ofsFromEnd < -6) ) {
            _UB_BLStart  = _config.memoryFlash.totalSize - blSize;
            _UB_BLEnd    = _config.memoryFlash.totalSize - 1;
            _UB_AppStart = 0;
            _UB_AppEnd   = _UB_BLStart - 1;
        }

        else {
            // Read the reset vector
            if( !_readFlash(buff, 4, 0) ) return false;

            final int rstInst = _toU16(buff, 0);

            // Check for 'RJMP'
            if( _avr_opcode_is_RJMP(rstInst) ) {
                if( XCom.isPowerOfTwo(_config.memoryFlash.totalSize) ) {
                    int pos = _avr_RJMP_ofsGet(rstInst);
                    while(pos < 0) pos += _config.memoryFlash.totalSize;
                    if( (_config.memoryFlash.totalSize - pos <= 2048) && _isFlashPageAligned(pos) ) {
                        _UB_BLStart  = pos;
                        _UB_BLEnd    = _config.memoryFlash.totalSize - 1;
                        _UB_AppStart = 0;
                        _UB_AppEnd   = _UB_BLStart - 1;
                    }
                }
            }

            // Check for 'JMP' (only for AVR MCUs with larger flash)
            else if( _avr_opcode_is_JMP(rstInst) && _hasGT8kFlash() ) {
                final int pos = _avr_JMP_addrGet( _toU32(buff, 0) );
                if( (pos < _config.memoryFlash.totalSize) && (_config.memoryFlash.totalSize - pos <= 2048) && _isFlashPageAligned(pos) ) {
                    _UB_BLStart  = pos;
                    _UB_BLEnd    = _config.memoryFlash.totalSize - 1;
                    _UB_AppStart = 0;
                    _UB_AppEnd   = _UB_BLStart - 1;
                }
            }
        }

        if(_UB_BLStart == -1) return USB2GPIO.notifyError(
            Texts.ProgXXX_FailBLResultXErrI(Texts.CmdXErr_BLStartAdrUnresol), ProgClassName
        );

        // Calculate the offset at which JxMake metadata can be placed
        if( _config.memoryFlash.pageSize >= _jxmakeMetadataSize() ) _UB_JxMakeMetaAddr = _UB_AppEnd + 1 - _config.memoryFlash.pageSize;
        else                                                        _UB_JxMakeMetaAddr = -1;

        // Read and dump the current metadata
        if( !_dumpMetadata() ) return false;

        // Done
        return true;
    }

    private int _getResetAddress(final int[] buff)
    {
        int addr = -1;

        if( _avr_opcode_is_RJMP( _toU16(buff, 0) ) ) {
            addr = _avr_RJMP_ofsGet( _toU16(buff, 0) );
            while(addr < 0                            ) addr += _config.memoryFlash.totalSize;
            while(addr > _config.memoryFlash.totalSize) addr -= _config.memoryFlash.totalSize;
        }

        else if( _avr_opcode_is_JMP( _toU16(buff, 0) ) ) {
            addr = _avr_JMP_addrGet( _toU32(buff, 0) );
        }

        return addr;
    }

    private boolean _putResetVector(final byte[] data)
    {
        // Check if patching is not required
        if(!_UB_IsVBL) return false;
        if(_UB_VBLSelfPatch || _UB_VBLSelfPatchVerify) return false;

        // Check whether the vector number for the '[R]JMP' to the application is valid
        if(_UB_AppJVNum <= 0) return false;

        // Convert to integer(s)
        final int[] buff = new int[data.length];

        USB2GPIO.ba2ia(buff, data);

        // Put the reset vector
        if( !_hasGT8kFlash() || _hasPowerOfTwoFlash() ) _putU16( buff, 0, _avr_opcode_gen_RJMP_toBLStart(_UB_BLStart) );
        else                                            _putU32( buff, 0, _avr_opcode_gen_JMP           (_UB_BLStart) );

        // Convert back to bytes(s)
        USB2GPIO.ia2ba(data, buff);

        // Done
        return true;
    }

    private boolean _patchVectors(final byte[] data)
    {
        // Check if patching is not required
        if(!_UB_IsVBL) return true;
        if(_UB_VBLSelfPatch || _UB_VBLSelfPatchVerify) return true;

        // Check whether the vector number for the '[R]JMP' to the application is valid
        if(_UB_AppJVNum <= 0) return false;

        // Convert to integer(s)
        final int[] buff = new int[data.length];

        USB2GPIO.ba2ia(buff, data);

        // Patch the vectors
        if( !_hasGT8kFlash() ) { // <= 8192

            // Ensure the vectors contain valid instructions
            for( int i = 0; i < ( _configMCUInfo().numVectors * 2 ); i += 2 ) {
                if( !_avr_opcode_is_RJMP( _toU16(buff, i) ) ) return false;
            }

            // Get and check the application start address
            final int appStart = _getResetAddress(buff);

            if(appStart == _UB_BLStart) return true;

            // Patch the 'Reset' vector to jump to the bootloader start
            _putU16( buff, 0, _avr_opcode_gen_RJMP_toBLStart(_UB_BLStart) );

            // Patch the 'Store Program Memory Ready' vector to jump to the application start
            _putU16( buff, _UB_AppJVNum * 2, _avr_opcode_gen_RJMP(appStart - _UB_AppJVNum * 2) );

        }

        else { // > 8192

            // Ensure the vectors contain valid instructions
            for( int i = 0; i < ( _configMCUInfo().numVectors * 4 ); i += 4 ) {
                if( !_avr_opcode_is_RJMP( _toU16(buff, i) ) && !_avr_opcode_is_JMP( _toU16(buff, i) ) ) return false;
            }

            // Get and check the application start address
            final int appStart = _getResetAddress(buff);

            if(appStart == _UB_BLStart) return true;

            // Patch the 'Reset' vector to jump to the bootloader start
            if( _hasPowerOfTwoFlash() ) _putU16( buff, 0, _avr_opcode_gen_RJMP_toBLStart(_UB_BLStart) );
            else                        _putU32( buff, 0, _avr_opcode_gen_JMP           (_UB_BLStart) );

            // Patch the 'Store Program Memory Ready' vector to jump to the application start
           _putU32( buff, _UB_AppJVNum * 4, _avr_opcode_gen_JMP(appStart) );

        }

        // Convert back to bytes(s)
        USB2GPIO.ia2ba(data, buff);

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _inProgMode  = false;
    private boolean _chipErased  = false;

    public ProgBootURCLOCK(final ProgBootURCLOCK.Config config) throws Exception
    {
        // Process the superclass
        super(ProgClassName, config);

        // Check the configuration values
        if( _configMCUInfo().numVectors <= 0 ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMINumVecs, ProgClassName);

        if(_config.memoryEEPROM.totalSize > 0) {
            if( _configMCUInfo().totalSRAM <= 0 ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMITotSRAM, ProgClassName);
        }
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
        if( !_openSerialPort(serialDevice, baudrate, null) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSerDev, ProgClassName);

        // Initialize the programmer
        if( !_urConnect     () ) return false;
        if( !_getSync       () ) return false;
        if( !_enterProgMode () ) return false;
        if( !_readParseTable() ) return false;

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
        final boolean resCommitEEPROM  = commitEEPROM();
        final boolean resLeaveProgMode = _leaveProgMode();

        // Close the serial port
        _closeSerialPort();

        // Reset all data
        _urResetData();

        // Clear flag
        _inProgMode = false;

        // Check for error(s)
        if(!resCommitEEPROM || !resLeaveProgMode) {
            if(!resCommitEEPROM ) USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_CmEEPROM, ProgClassName);
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

        // NOTE : The MCU signature was read during 'begin()'

        // Done
        return _mcuSignature != null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : Under the URCLOCK protocol, this function is expected to erase only flash memory
    @Override
    public boolean chipErase()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_chipErased) return true;

        // The bootloader supports the chip erase command - use it
        if(_UB_ChipErase) {

            // Prepare the command
            _pbs_wrBuff_32I[0] = CMD_STK_CHIP_ERASE;
            _pbs_wrBuff_32I[1] = SYN_CRC_EOP;

            // Executes the command and returns its success or failure state
            _setSerialRxTimeout_MS_long(); // Set a much longer Rx timeout

            final boolean res = _execCmd(_pbs_wrBuff_32I, 2);

            _setSerialRxTimeout_MS_default(); // Restore the default Rx timeout

            if(!res) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        }

        // The bootloader does not support the chip erase command - try to emulate it
        else {

            // Prepare the data buffer
            final int    vectorSize = _configMCUInfo().numVectors * ( _hasGT8kFlash() ? 4 : 2 );
            final int    dataSize   = ( (vectorSize + _config.memoryFlash.pageSize - 1) / _config.memoryFlash.pageSize ) * _config.memoryFlash.pageSize;
            final byte[] data       = new byte[dataSize];

            Arrays.fill(data, FlashMemory_EmptyValue);

            // Only write the data buffer if the reset vector can be placed
            if( !_putResetVector(data) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            if( !_writeFlashPage_impl(data, 0, data.length, null) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        }

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

        // Patch the data
        if(refData != null) _patchVectors(refData);

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

            if( !_readFlash(cbytes, ChunkSize, sa + c * ChunkSize) ) {
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
            if( !_writeFlash( USB2GPIO.ba2ia(data, datIdx, _config.memoryFlash.pageSize), cpgAddr ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

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

        // Patch the data
        _patchVectors(data);

        // Align the number of bytes and pad the buffer as needed
        final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer(data, sa, nb, _config.memoryFlash.pageSize, _config.memoryFlash.totalSize, FlashMemory_EmptyValue, ProgClassName);
        if(anbr == null) return false;

        // Write flash
        if( !_writeFlashPage_impl(anbr.buff, sa, anbr.nb, progressCallback) ) return false;

        // Write metadata if space is available
        if(_UB_JxMakeMetaAddr >= startAddress + anbr.nb) {
            return _writeFlash(  _jxmakeMetadataGet(data.length), _UB_JxMakeMetaAddr );
        }

        // Done
        return true;
    }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash_impl(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : Not all bootloader implementations that use the URCLOCK protocol support EEPROM reading and writing!

    private int[]     _eepromBuffer = null;
    private boolean[] _eepromFDirty = null;

    private boolean _readAllEEPROMBytes()
    {
        // Prepare the result buffer
        if(_eepromBuffer == null) _eepromBuffer = new int[_config.memoryEEPROM.totalSize];

        // Determine the chunk size and the number of chunks
        final int ChunkSize = Math.min( _config.memoryEEPROM.totalSize, Math.min( _configMCUInfo().totalSRAM / 2, _config.memoryFlash.pageSize ) );
        final int numChunks = (_config.memoryEEPROM.totalSize / ChunkSize);

        // Read the bytes
        int rdbIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk
            final int numReads = Math.min(ChunkSize, _config.memoryEEPROM.totalSize - rdbIdx);

            // Read the chunk bytes
            final int[] cbytes = new int[numReads];

            if( !_readEEPROM(cbytes, ChunkSize, c * ChunkSize) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                return false;
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
        final int ChunkSize = Math.min( _config.memoryEEPROM.totalSize, Math.min( _configMCUInfo().totalSRAM / 2, _config.memoryFlash.pageSize ) );
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
            if( !_writeEEPROM(cbytes, c * ChunkSize) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                return false;
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

} // class ProgBootURCLOCK
