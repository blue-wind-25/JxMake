/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;

import java.lang.reflect.Constructor;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.function.IntConsumer;
import java.util.stream.LongStream;

import com.fazecast.jSerialComm.*;

import jxm.*;
import jxm.xb.*;

import static jxm.ugc.ARMCortexMThumb.CPU;
import static jxm.ugc.ARMCortexMThumb.Reg;


/*
 * Simple implementation of the SAM-BA protocol for programming MCUs with compatible bootloaders.
 * (e.g., 'at91sam3x8e'). Limitations:
 *     1. Does not support setting the security bit and lock regions.
 *     2. Does not support setting custom option bits (default option bits are always selected).
 *     3. Does not support setting calibration bits, etc.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written mostly based on the algorithms and information found from:
 *
 *     BOSSA
 *     https://github.com/shumatech/BOSSA/tree/master
 *     Copyright (C) 2011-2018, ShumaTech
 *     BSD 3-Clause License
 *
 *     Carioca
 *     https://pypi.org/project/carioca
 *     Copyright (C) 2016-2024 eGauge Systems LLC
 *     MIT License
 *
 *     The SAM-BA Protocol
 *     https://sourceforge.net/p/lejos/wiki-nxt/SAM-BA%20Protocol
 *
 *     SAM-BA User Guide
 *     https://support.garz-fricke.com/products/Neso/Linux/Documentation/sam-ba%20user%20guide.pdf
 *
 *     SAM Boot Assistant (SAM-BA) User Guide
 *     https://www.keelog.com/files/SambaUserGuide.pdf
 */
public class ProgBootSAMBA extends ProgBootSerial {

    private static final String ProgClassName    = "ProgBootSAMBA";

    public  static final int    DefMagicBaudrate = 1200;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class Config extends ProgBootSerial.Config {

        public static class MCUInfo implements Serializable {
            public long[] chipID    = null; // Optional
            public long[] extChipID = null; // ---
            public long[] deviceID  = null; // ---
        }

        public static class AppletInfo implements Serializable {
            public String  flashDriver      = null;
            public long    flashBase        = -1;
            public int     flashNumPlanes   = -1;
            public int     flashNumLockRegs = -1;

            public long    flashBLStart     = -1; // Optional; only required for MCUs with the SAM-BA bootloader
            public int     flashBLSize      = -1; // in flash rather than ROM. It is typically 4kB if only USB or
                                                  // UART is enabled, and 8kB when both interfaces are enabled.
                                                  // Custom SAM-BA bootloaders may have larger size.

            public long    sramAppletStart  = -1;
            public long    sramAppletStack  = -1;

            public long    regBase          = -1;
            public long    regReset         = -1;
            public long    regResetCommand  = -1;


            public boolean canBrownout      = false; // Typically 'true' for most MCUs; however, some MCUs with
                                                     // EEFC may require this to be 'false'.

            public boolean canBootFlash     = false; // Typically 'false' for MCUs with NVMCTRL; 'true' for MCUs
                                                     // with EFC and EEFC, though certain MCUs with EFC may require
                                                     // this to be 'false'.
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final MCUInfo    mcuInfo    = new MCUInfo   ();
        public final AppletInfo appletInfo = new AppletInfo();

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'xxx*()' functions ??? #####


public static Config ATSAM3A4()
{
    final Config config = new Config();

    config.memoryFlash.totalSize       = 262144;
    config.memoryFlash.pageSize        =    256;
    config.memoryFlash.numPages        =   1024;

    config.memoryEEPROM.totalSize      = 0;

    //                                                3A4C
    config.mcuInfo.chipID              = new long[] { 0x283B0960L };

    config.appletInfo.flashDriver      = "EEFC";
    config.appletInfo.flashBase        = 0x00080000L;
    config.appletInfo.flashNumPlanes   =  2;
    config.appletInfo.flashNumLockRegs = 16;

    config.appletInfo.sramAppletStart  = 0x20001000L;
    config.appletInfo.sramAppletStack  = 0x20008000L;

    config.appletInfo.regBase          = 0x400E0A00L;
    config.appletInfo.regReset         = 0x400E1A00L;
    config.appletInfo.regResetCommand  = 0xA500000DL;

    config.appletInfo.canBrownout      = false;
    config.appletInfo.canBootFlash     = true;

    return config;
}

public static Config ATSAM3A8()
{
    final Config config = ATSAM3A4();

    config.memoryFlash.totalSize       = 524288;
    config.memoryFlash.numPages        =   2048;

    //                                                3A8C
    config.mcuInfo.chipID              = new long[] { 0x283E0A60L };

    config.appletInfo.flashNumLockRegs = 32;

    config.appletInfo.sramAppletStack  = 0x20010000L;

    return config;
}

public static Config ATSAM3X4()
{
    final Config config = ATSAM3A4();

    //                                                3X4C         3X4E
    config.mcuInfo.chipID              = new long[] { 0x284B0960L, 0x285B0960L };

    return config;
}

public static Config ATSAM3X8()
{
    final Config config = ATSAM3A8();

    //                                                3X8C         3X8E         3X8H
    config.mcuInfo.chipID              = new long[] { 0x284E0A60L, 0x285E0A60L, 0x286E0A60L };

    return config;
}

public static Config ArduinoDue  () { return ATSAM3X8(); }
public static Config DiymoreDueR3() { return ATSAM3X8(); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATSAMD21x15()
{
    final Config config = new Config();

    config.memoryFlash.totalSize       = 32768;
    config.memoryFlash.pageSize        =    64;
    config.memoryFlash.numPages        =   512;

    config.memoryEEPROM.totalSize      = 0;

    config.mcuInfo.deviceID            = new long[] {
                                             0x1001000DL, // E15A
                                             0x10010027L, // E15B
                                             0x10010056L, // E15B WLCSP
                                             0x10010063L, // E15C WLCSP
                                             0x10010008L, // G15A
                                             0x10010024L, // G15B
                                             0x10010003L, // J15A
                                             0x10010021L  // J15B
                                         };

    config.appletInfo.flashDriver      = "D2X";
    config.appletInfo.flashBase        = 0x00000000L;
    config.appletInfo.flashNumPlanes   =  1;
    config.appletInfo.flashNumLockRegs = 16;

    config.appletInfo.flashBLStart     = 0x00000000L; // NOTE : Please adjust this as required
    config.appletInfo.flashBLSize      = 0x2000;

    config.appletInfo.sramAppletStart  = 0x20000800L;
    config.appletInfo.sramAppletStack  = 0x20001000L;

    config.appletInfo.regBase          = 0x41004000L;
    config.appletInfo.regReset         = 0xE000ED0CL;
    config.appletInfo.regResetCommand  = 0x05FA0004L;

    config.appletInfo.canBrownout      = true;
    config.appletInfo.canBootFlash     = false;

    return config;
}

public static Config ATSAMD21x16()
{
    final Config config = ATSAMD21x15();

    config.memoryFlash.totalSize       = 65536;
    config.memoryFlash.numPages        =  1024;

    config.mcuInfo.deviceID            = new long[] {
                                             0x1001000CL, // E16A
                                             0x10010026L, // E16B
                                             0x10010055L, // E16B WLCSP
                                             0x10010062L, // E16C WLCSP
                                             0x10010007L, // G16A
                                             0x10010023L, // G16B
                                             0x10010002L, // J16A
                                             0x10010020L  // J16B
                                         };

    config.appletInfo.sramAppletStart  = 0x20001000L;
    config.appletInfo.sramAppletStack  = 0x20002000L;

    return config;
}

public static Config ATSAMD21x17()
{
    final Config config = ATSAMD21x15();

    config.memoryFlash.totalSize       = 131072;
    config.memoryFlash.numPages        =   2048;

    config.mcuInfo.deviceID            = new long[] {
                                             0x1001000BL, // E17A
                                             0x10010006L, // G17A
                                             0x10010010L, // G17A WLCSP
                                             0x10010001L  // J17A
                                         };

    config.appletInfo.sramAppletStart  = 0x20002000L;
    config.appletInfo.sramAppletStack  = 0x20004000L;

    return config;
}

public static Config ATSAMD21x18()
{
    final Config config = ATSAMD21x15();

    config.memoryFlash.totalSize       = 262144;
    config.memoryFlash.numPages        =   4096;

    config.mcuInfo.deviceID            = new long[] {
                                             0x1001000AL, // E18A
                                             0x10010005L, // G18A
                                             0x1001000FL, // G18A WLCSP
                                             0x10010000L  // J18A
                                         };

    config.appletInfo.sramAppletStart  = 0x20004000L;
    config.appletInfo.sramAppletStack  = 0x20008000L;

    return config;
}

public static Config ArduinoZero       () { return ATSAMD21x18(); }
public static Config ArduinoMKRZero    () { return ATSAMD21x18(); }

public static Config SeeeduinoXIAO     () { return ATSAMD21x18(); }
public static Config AdafruitFeatherM0 () { return ATSAMD21x18(); }
public static Config SparkFunSAMD21Mini() { return ATSAMD21x18(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    public Config.MemoryFlash _configMemoryFlash()
    { return ( (Config) super._config ).memoryFlash; }

    public Config.MCUInfo _configMCUInfo()
    { return ( (Config) super._config ).mcuInfo; }

    public Config.AppletInfo _configAppletInfo()
    { return ( (Config) super._config ).appletInfo; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static abstract class FlashOper {

        protected final ProgBootSAMBA _samba;

        protected final long          _flashBase;
        protected final int           _flashPages;
        protected final int           _flashPageSize;
        protected final int           _flashPlanes;
        protected final int           _flashLockRegions;

        protected final long          _appletStart;
        protected final long          _appletStack;

        protected final long          _regBase;

        protected final boolean       _canBrownout;
        protected final boolean       _canBootFlash;

        protected       boolean       _pageBufferASel;
        protected final long          _pageBufferA;
        protected final long          _pageBufferB;

        protected FlashOper(
            final ProgBootSAMBA instProgBootSAMBA,
            final long          flashBaseAddress,
            final int           flashNumberOfPages,
            final int           flashBytePageSize,
            final int           flashNumberOfPlanes,
            final int           flashNumberOfLockRegions,
            final long          sramAppletStartAddress,
            final long          sramAppletStackAddress,
            final long          regBase,
            final boolean       canBrownout,
            final boolean       canBootFlash
        )
        {
            _samba            = instProgBootSAMBA;

            _flashBase        = flashBaseAddress;
            _flashPages       = flashNumberOfPages;
            _flashPageSize    = flashBytePageSize;
            _flashPlanes      = flashNumberOfPlanes;
            _flashLockRegions = flashNumberOfLockRegions;

            _appletStart      = sramAppletStartAddress;
            _appletStack      = sramAppletStackAddress;

            _regBase          = regBase;

            _canBrownout      = canBrownout;
            _canBootFlash     = canBootFlash;

            _pageBufferASel   = true;
            _pageBufferA      = ( ( _appletStart + WordCopyARM.size() + 3 ) / 4 ) * 4; // Align to 32-bit
            _pageBufferB      = _pageBufferA + _flashPageSize;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // ##### !!! TODO : Add more features (security bit, custom option bits, lock regions, etc.) !!! #####

        public boolean loadBuffer(final int[] buffer, final int length)
        { return _samba._writeBuff(_pageBufferASel ? _pageBufferA : _pageBufferB, buffer, length); }

        public boolean loadBuffer(final int[] buffer)
        { return loadBuffer(buffer, buffer.length); }

        public abstract boolean eraseAll(final long startOffset);
        public abstract boolean writePage(final int pageNum, final boolean eraseBeforeWrite);
        public abstract boolean readPage(final int pageNum, final int[] buffer);

        public abstract boolean writeOptions();

    } // abstract class FlashOper
public static class FlashD2X extends FlashOper {

    // NOTE : This class was adapted from the BOSSA source code

    private static final int  NVM_REG_CTRLA              = 0x00; // Offset from '_regBase'
    private static final int  NVM_REG_CTRLB              = 0x04; // ---
    private static final int  NVM_REG_INTFLAG            = 0x14; // ---
    private static final int  NVM_REG_STATUS             = 0x18; // ---
    private static final int  NVM_REG_ADDR               = 0x1C; // ---
    private static final int  NVM_REG_LOCK               = 0x20; // ---

    private static final int  CMDEX_KEY                  = 0xA500;

    private static final int  NVM_INT_STATUS_READY_MASK  = 0x0001;
    private static final int  NVM_INT_STATUS_ERROR_MASK  = 0x0002;
    private static final int  NVM_CTRL_STATUS_MASK       = 0xFFEB;

    private static final int  NVM_CMD_ER                 = 0x02;
    private static final int  NVM_CMD_WP                 = 0x04;
    private static final int  NVM_CMD_EAR                = 0x05;
    private static final int  NVM_CMD_WAP                = 0x06;
    private static final int  NVM_CMD_LR                 = 0x40;
    private static final int  NVM_CMD_UR                 = 0x41;
    private static final int  NVM_CMD_SSB                = 0x45;
    private static final int  NVM_CMD_PBC                = 0x44;

    private static final int  ERASE_ROW_PAGES            = 4;

    private static final long NVM_UR_ADDR                = 0x00804000L;
    private        final int  NVM_UR_SIZE                             ;
    private static final int  NVM_UR_BOD33_ENABLE_OFFSET = 0x01;
    private static final int  NVM_UR_BOD33_ENABLE_MASK   = 0x06;
    private static final int  NVM_UR_BOD33_RESET_OFFSET  = 0x01;
    private static final int  NVM_UR_BOD33_RESET_MASK    = 0x07;
    private static final int  NVM_UR_NVM_LOCK_OFFSET     = 0x06;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FlashD2X(
        final ProgBootSAMBA instProgBootSAMBA,
        final long          flashBaseAddress,
        final int           flashNumberOfPages,
        final int           flashBytePageSize,
        final int           flashNumberOfPlanes,
        final int           flashNumberOfLockRegions,
        final long          sramAppletStartAddress,
        final long          sramAppletStackAddress,
        final long          regBase,
        final boolean       canBrownout,
        final boolean       canBootFlash
    )
    {
        // Call the superclass constructor
        super(
            instProgBootSAMBA     ,
            flashBaseAddress      , flashNumberOfPages    , flashBytePageSize, flashNumberOfPlanes, flashNumberOfLockRegions,
            sramAppletStartAddress, sramAppletStackAddress,
            regBase               ,
            canBrownout           , canBootFlash
        );

        // Calculate the USER_ROW size
        NVM_UR_SIZE = flashBytePageSize * ERASE_ROW_PAGES;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private long _readReg(final int regOfs)
    { return _samba._readU32(_regBase + regOfs); }

    private void _writeReg(final int regOfs, final long value)
    { _samba._writeU32(_regBase + regOfs, value); }

    private boolean _waitReady()
    {
        while(true) {

            final long res = _readReg(NVM_REG_INTFLAG);
            if(res < 0) return false;

            if( (res & NVM_INT_STATUS_READY_MASK) != 0 ) break;
        }

        return true;
    }

    private boolean _execCmd(final int cmd)
    {
        if( !_waitReady() ) return false;
        _writeReg(NVM_REG_CTRLA, CMDEX_KEY | cmd);
        if( !_waitReady() ) return false;

        final long res = _readReg(NVM_REG_INTFLAG);
        if(res < 0) return false;

        if( (res & NVM_INT_STATUS_ERROR_MASK) != 0 ) {
            _writeReg(NVM_REG_INTFLAG, NVM_INT_STATUS_ERROR_MASK); // Clear the error bit
            return false;
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _inBLArea(final long start, final int size)
    {
        final long blStart = _samba._configAppletInfo().flashBLStart;
        final int  blSize  = _samba._configAppletInfo().flashBLSize;

        if(blStart < 0 || blSize <= 0) return false;

        return (start >= blStart) && ( (start + size) <= (blStart + blSize) );
    }

    private boolean _erase_impl(final long start_, final long size_)
    {
        // Check the start and size
        final int blockSize = _flashPageSize * ERASE_ROW_PAGES;

        if( (start_ % blockSize) != 0 ) return false;

        if( (start_ + size_) > _samba._configMemoryFlash().totalSize ) return false;

        // Trim leading and trailing blocks that fall inside bootloader area
        long start = start_;
        long size  = size_;

        while( size > 0 && _inBLArea(start, blockSize) ) {
            start += blockSize;
            size  -= blockSize;
        }

        while( size > 0 && _inBLArea(start + size - blockSize, blockSize) ) {
            size -= blockSize;
        }

        if(size <= 0) return true; // Nothing left to erase - skip silently

        // Perform erase
        final long endBlock = (start + size + blockSize - 1) / blockSize;

        if( !_waitReady() ) return false;

        for(long eraseBlock = start / blockSize; eraseBlock < endBlock; ++eraseBlock) {

            // Clear the error bits
            final long res = _readReg(NVM_REG_STATUS);
            if(res < 0) return false;

            _writeReg(NVM_REG_STATUS, NVM_CTRL_STATUS_MASK);

            // Issue erase command
            _writeReg( NVM_REG_ADDR, (eraseBlock * blockSize) / 2 );
            if( !_execCmd(NVM_CMD_ER) ) return false;

            if( !_waitReady() ) return false;

        } // for

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean eraseAll(final long startOffset)
    { return _erase_impl( startOffset, _samba._configMemoryFlash().totalSize - startOffset ); }

    @Override
    public boolean writePage(final int pageNum, final boolean eraseBeforeWrite)
    {
        if(pageNum >= _flashPages) return false;

        // Set the CACHEDIS and MANW bits (disable cache and enable manual page write)
        final long resCTRLB = _readReg(NVM_REG_CTRLB);
        if(resCTRLB < 0) return false;

        _writeReg( NVM_REG_CTRLB, resCTRLB | (1L << 18) | (1L << 7) );

        // Perform erase if writing at the start of the erase page
        if(eraseBeforeWrite && pageNum % ERASE_ROW_PAGES == 0) {
            if( !_erase_impl(pageNum * _flashPageSize, ERASE_ROW_PAGES * _flashPageSize) ) return false;
        }

        // Clear page buffer
        if( !_execCmd(NVM_CMD_PBC) ) return false;

        // Calculate and check the address
        final long dstAddr = _flashBase + (pageNum * _flashPageSize);

        if( _inBLArea(dstAddr, _flashPageSize) ) return true; // Skip silently

        // Perform the write
        if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
        if( !_samba._applet_setDstAddr(dstAddr                                      ) ) return false;
        _pageBufferASel = !_pageBufferASel;

        if( !_waitReady() ) return false;

        if( !_samba._applet_runT2() ) return false;

        _writeReg(NVM_REG_ADDR, dstAddr / 2);
        if( !_execCmd(NVM_CMD_WP) ) return false;

        // Done
        return true;
    }

    @Override
    public boolean readPage(final int pageNum, final int[] buffer)
    {
        if(pageNum >= _flashPages) return false;

        return _samba._readBuff( _flashBase + (pageNum * _flashPageSize), buffer, _flashPageSize );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Add more features (security bit, custom option bits, lock regions, etc.) !!! #####

    public boolean writeOptions()
    {
        // NOTE : # This class will never set the 'Security' bit and lock region(s)!
        //        # The '_canBootFlash' flag is unused in this class.

        // Read the entire USER_ROW
        final int[] orgUserRow = new int[NVM_UR_SIZE];

        if( !_samba._readBuff(NVM_UR_ADDR, orgUserRow) ) return false;

        /* Modify the USER_ROW (set the default option bits)
         *
         * SAM D21/DA1 Family
         * https://ww1.microchip.com/downloads/aemDocuments/documents/MCU32/ProductDocuments/DataSheets/SAM-D21-DA1-Family-Data-Sheet-DS40001882.pdf
         *
         * SAM R21E/R21G
         * https://ww1.microchip.com/downloads/en/DeviceDoc/SAM-R21_Datasheet.pdf
         */
        final int[] modUserRow = Arrays.copyOf(orgUserRow, orgUserRow.length);

        if(_canBrownout) {
            modUserRow[NVM_UR_BOD33_ENABLE_OFFSET] |= NVM_UR_BOD33_ENABLE_MASK; // Enable BOD
            modUserRow[NVM_UR_BOD33_RESET_OFFSET ] |= NVM_UR_BOD33_RESET_MASK ; // Enable BOR
        }

        if( Arrays.equals(modUserRow, orgUserRow) ) return true;

        // Write the USER_ROW
        if(true) {

            // Set the CACHEDIS and MANW bits (disable cache and enable manual page write)
            final long resCTRLB = _readReg(NVM_REG_CTRLB);
            if(resCTRLB < 0) return false;

            _writeReg( NVM_REG_CTRLB, resCTRLB | (1L << 18) | (1L << 7) );

            // Erase user row
            _writeReg(NVM_REG_ADDR, NVM_UR_ADDR / 2);
            if( !_execCmd(NVM_CMD_EAR) ) return false;

            for(int ofs = 0; ofs < NVM_UR_SIZE; ofs += _flashPageSize) {

                // Load the buffer with the chunk
                final int[] slice = Arrays.copyOfRange(modUserRow, ofs, ofs + _flashPageSize);
                if( !loadBuffer(slice) ) return false;

                // Clear page buffer
                if( !_execCmd(NVM_CMD_PBC) ) return false;

                // Perform the write
                if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
                if( !_samba._applet_setDstAddr(NVM_UR_ADDR + ofs                            ) ) return false;
                _pageBufferASel = !_pageBufferASel;

                if( !_waitReady() ) return false;

                if( !_samba._applet_runT2() ) return false;

                _writeReg( NVM_REG_ADDR, (NVM_UR_ADDR + ofs) / 2 );
                if( !_execCmd(NVM_CMD_WAP) ) return false;

            } // for

        } // if

        // Done
        return true;
    }

} // class FlashD2X


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
// ##### !!! TODO : VERIFY !!! #####

public static class FlashD5X extends FlashOper {

    // NOTE : This class was adapted from the BOSSA source code

    private static final int  NVM_REG_CTRLA               = 0x00; // Offset from '_regBase'
    private static final int  NVM_REG_CTRLB               = 0x04; // ---
    private static final int  NVM_REG_INTFLAG             = 0x10; // ---
    private static final int  NVM_REG_STATUS              = 0x12; // ---
    private static final int  NVM_REG_ADDR                = 0x14; // ---
    private static final int  NVM_REG_RUNLOCK             = 0x18; // ---

    private static final int  CMDEX_KEY                   = 0xA500;

    private static final int  NVM_INT_STATUS_READY_MASK   = 0x0001;
    private static final int  NVM_INT_STATUS_ERROR_MASK   = 0x00CE;

    private static final int  NVM_CMD_EP                  = 0x00;
    private static final int  NVM_CMD_EB                  = 0x01;
    private static final int  NVM_CMD_WP                  = 0x03;
    private static final int  NVM_CMD_WQW                 = 0x04;
    private static final int  NVM_CMD_LR                  = 0x11;
    private static final int  NVM_CMD_UR                  = 0x12;
    private static final int  NVM_CMD_SSB                 = 0x16;
    private static final int  NVM_CMD_PBC                 = 0x15;

    private static final int  ERASE_BLOCK_PAGES           = 16;

    private static final long NVM_UP_ADDR                 = 0x00804000L;
    private        final int  NVM_UP_SIZE                              ;
    private static final int  NVM_UP_BOD33_DISABLE_OFFSET = 0x00;
    private static final int  NVM_UP_BOD33_DISABLE_MASK   = 0x01;
    private static final int  NVM_UP_BOD33_RESET_OFFSET   = 0x01;
    private static final int  NVM_UP_BOD33_RESET_MASK     = 0x02;
    private static final int  NVM_UP_NVM_LOCK_OFFSET      = 0x08;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FlashD5X(
        final ProgBootSAMBA instProgBootSAMBA,
        final long          flashBaseAddress,
        final int           flashNumberOfPages,
        final int           flashBytePageSize,
        final int           flashNumberOfPlanes,
        final int           flashNumberOfLockRegions,
        final long          sramAppletStartAddress,
        final long          sramAppletStackAddress,
        final long          regBase,
        final boolean       canBrownout,
        final boolean       canBootFlash
    )
    {
        // Call the superclass constructor
        super(
            instProgBootSAMBA     ,
            flashBaseAddress      , flashNumberOfPages    , flashBytePageSize, flashNumberOfPlanes, flashNumberOfLockRegions,
            sramAppletStartAddress, sramAppletStackAddress,
            regBase               ,
            canBrownout           , canBootFlash
        );

        // Set the USER_PAGE size
        NVM_UP_SIZE = flashBytePageSize;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _readRegU16(final int regOfs)
    {
        if(!true) {
            return _samba._readU16(_regBase + regOfs);
        }
        else {
            return _samba._readU08(_regBase + regOfs) | ( _samba._readU08(_regBase + regOfs + 1) << 8 );
        }
    }

    private void _writeRegU16(final int regOfs, final int value)
    {
        if(!true) {
            _samba._writeU16(_regBase + regOfs, value);
        }
        else {
            _samba._writeU08( _regBase + regOfs    ,  value       & 0xFF );
            _samba._writeU08( _regBase + regOfs + 1, (value >> 8) & 0xFF );
        }
    }

    private long _readRegU32(final int regOfs)
    { return _samba._readU32(_regBase + regOfs); }

    private void _writeRegU32(final int regOfs, final long value)
    { _samba._writeU32(_regBase + regOfs, value); }

    private boolean _waitReady()
    {
        while(true) {

            final int res = _readRegU16(NVM_REG_STATUS);
            if(res < 0) return false;

            if( (res & NVM_INT_STATUS_READY_MASK) != 0 ) break;
        }

        return true;
    }

    private boolean _execCmd(final int cmd)
    {
        if( !_waitReady() ) return false;
        _writeRegU32(NVM_REG_CTRLB, CMDEX_KEY | cmd);
        if( !_waitReady() ) return false;

        final int res = _readRegU16(NVM_REG_INTFLAG);
        if(res < 0) return false;

        if( (res & NVM_INT_STATUS_ERROR_MASK) != 0 ) {
            _writeRegU16(NVM_REG_INTFLAG, NVM_INT_STATUS_ERROR_MASK); // Clear the error bit
            return false;
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _inBLArea(final long start, final int size)
    {
        final long blStart = _samba._configAppletInfo().flashBLStart;
        final int  blSize  = _samba._configAppletInfo().flashBLSize;

        if(blStart < 0 || blSize <= 0) return false;

        return (start >= blStart) && ( (start + size) <= (blStart + blSize) );
    }

    private boolean _erase_impl(final long start_, final long size_)
    {
        // Check the start and size
        final int blockSize = _flashPageSize * ERASE_BLOCK_PAGES;

        if( (start_ % blockSize) != 0 ) return false;

        if( (start_ + size_) > _samba._configMemoryFlash().totalSize ) return false;

        // Trim leading and trailing blocks that fall inside bootloader area
        long start = start_;
        long size  = size_;

        while( size > 0 && _inBLArea(start, blockSize) ) {
            start += blockSize;
            size  -= blockSize;
        }

        while( size > 0 && _inBLArea(start + size - blockSize, blockSize) ) {
            size -= blockSize;
        }

        if(size <= 0) return true; // Nothing left to erase - skip silently

        // Perform erase
        final long endBlock = (start + size + blockSize - 1) / blockSize;

        if( !_waitReady() ) return false;

        for(long eraseBlock = start / blockSize; eraseBlock < endBlock; ++eraseBlock) {

            // Issue erase command
            _writeRegU32(NVM_REG_ADDR, eraseBlock * blockSize);
            if( !_execCmd(NVM_CMD_EB) ) return false;

            if( !_waitReady() ) return false;

        } // for

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean eraseAll(final long startOffset)
    { return _erase_impl( startOffset, _samba._configMemoryFlash().totalSize - startOffset ); }

    @Override
    public boolean writePage(final int pageNum, final boolean eraseBeforeWrite)
    {
        if(pageNum >= _flashPages) return false;

        // Set the CACHEDIS[1:0] and clear the WMODE[1:0] bits (disable cache and enable manual page write)
        final int resCTRLA = _readRegU16(NVM_REG_CTRLA);
        if(resCTRLA < 0) return false;

        _writeRegU16( NVM_REG_CTRLA, ( resCTRLA | (0x3 << 14) ) & 0xFFCF );

        // Perform erase if writing at the start of the erase page
        if(eraseBeforeWrite && pageNum % ERASE_BLOCK_PAGES == 0) {
            if( !_erase_impl(pageNum * _flashPageSize, ERASE_BLOCK_PAGES * _flashPageSize) ) return false;
        }

        // Clear page buffer
        if( !_execCmd(NVM_CMD_PBC) ) return false;

        // Calculate and check the address
        final long dstAddr = _flashBase + (pageNum * _flashPageSize);

        if( _inBLArea(dstAddr, _flashPageSize) ) return true; // Skip silently

        // Perform the write
        if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
        if( !_samba._applet_setDstAddr(dstAddr                                      ) ) return false;
        if( !_samba._applet_setWords  (_flashPageSize / 4                           ) ) return false;
        _pageBufferASel = !_pageBufferASel;

        if( !_waitReady() ) return false;

        if( !_samba._applet_runT2() ) return false;

        _writeRegU32(NVM_REG_ADDR, dstAddr);
        if( !_execCmd(NVM_CMD_WP) ) return false;

        // Done
        return true;
    }

    @Override
    public boolean readPage(final int pageNum, final int[] buffer)
    {
        if(pageNum >= _flashPages) return false;

        return _samba._readBuff( _flashBase + (pageNum * _flashPageSize), buffer, _flashPageSize );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Add more features (security bit, custom option bits, lock regions, etc.) !!! #####

    public boolean writeOptions()
    {
        // NOTE : # This class will never set the 'Security' bit and lock region(s)!
        //        # The '_canBootFlash' flag is unused in this class.

        // Read the entire USER_PAGE
        final int[] orgUserPage = new int[NVM_UP_SIZE];

        if( !_samba._readBuff(NVM_UP_ADDR, orgUserPage) ) return false;

        /* Modify the USER_PAGE (set the default option bits)
         *
         * SAM D5x/E5x Family
         * https://ww1.microchip.com/downloads/aemDocuments/documents/MCU32/ProductDocuments/DataSheets/SAM-D5x-E5x-Family-Data-Sheet-DS60001507.pdf
         */
        final int[] modUserPage = Arrays.copyOf(orgUserPage, orgUserPage.length);

        if(_canBrownout) {
            modUserPage[NVM_UP_BOD33_DISABLE_OFFSET] &= ~NVM_UP_BOD33_DISABLE_MASK; // Enable BOD
            modUserPage[NVM_UP_BOD33_RESET_OFFSET  ] |=  NVM_UP_BOD33_RESET_MASK  ; // Enable BOR
        }

        if( Arrays.equals(modUserPage, orgUserPage) ) return true;

        // Write the USER_PAGE
        if(true) {

            // Set the CACHEDIS[1:0] and clear the WMODE[1:0] bits (disable cache and enable manual page write)
            final int resCTRLA = _readRegU16(NVM_REG_CTRLA);
            if(resCTRLA < 0) return false;

            _writeRegU16( NVM_REG_CTRLA, ( resCTRLA | (0x3 << 14) ) & 0xFFCF );

            // Erase user page
            _writeRegU32(NVM_REG_ADDR, NVM_UP_ADDR);
            if( !_execCmd(NVM_CMD_EP) ) return false;

            for(int ofs = 0; ofs < NVM_UP_SIZE; ofs += 16) {

                // Load the buffer with the chunk
                final int[] slice = Arrays.copyOfRange(modUserPage, ofs, ofs + 16);
                if( !loadBuffer(slice) ) return false;

                // Clear page buffer
                if( !_execCmd(NVM_CMD_PBC) ) return false;

                // Perform the write
                if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
                if( !_samba._applet_setDstAddr(NVM_UP_ADDR + ofs                            ) ) return false;
                if( !_samba._applet_setWords  (4                                            ) ) return false;
                _pageBufferASel = !_pageBufferASel;

                if( !_waitReady() ) return false;

                if( !_samba._applet_runT2() ) return false;

                _writeRegU32(NVM_REG_ADDR, NVM_UP_ADDR + ofs);
                if( !_execCmd(NVM_CMD_WQW) ) return false;

            } // for

        } // if

        // Done
        return true;
    }

} // class FlashD5X


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
// ##### !!! TODO : VERIFY !!! #####

public static class FlashEFC extends FlashOper {

    // NOTE : This class was adapted from the BOSSA source code

    private static final long EFC0_FMR      = 0xFFFFFF60L;
    private static final long EFC0_FCR      = 0xFFFFFF64L;
    private static final long EFC0_FSR      = 0xFFFFFF68L;

    private static final long EFC1_FMR      = 0xFFFFFF70L;
    private static final long EFC1_FCR      = 0xFFFFFF74L;
    private static final long EFC1_FSR      = 0xFFFFFF78L;

    private static final long EFC_KEY       = 0x5A;

    private static final long EFC_FCMD_WP   = 0x01;
    private static final long EFC_FCMD_SLB  = 0x02;
    private static final long EFC_FCMD_WPL  = 0x03;
    private static final long EFC_FCMD_CLB  = 0x04;
    private static final long EFC_FCMD_EA   = 0x08;
    private static final long EFC_FCMD_SGPB = 0x0B;
    private static final long EFC_FCMD_CGPB = 0x0D;
    private static final long EFC_FCMD_SSB  = 0x0F;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FlashEFC(
        final ProgBootSAMBA instProgBootSAMBA,
        final long          flashBaseAddress,
        final int           flashNumberOfPages,
        final int           flashBytePageSize,
        final int           flashNumberOfPlanes,
        final int           flashNumberOfLockRegions,
        final long          sramAppletStartAddress,
        final long          sramAppletStackAddress,
        final long          regBase,
        final boolean       canBrownout,
        final boolean       canBootFlash
    )
    {
        // Call the superclass constructor
        super(
            instProgBootSAMBA     ,
            flashBaseAddress      , flashNumberOfPages    , flashBytePageSize, flashNumberOfPlanes, flashNumberOfLockRegions,
            sramAppletStartAddress, sramAppletStackAddress,
            regBase               ,
            canBrownout           , canBootFlash
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _writeFCR0(final long cmd, final long arg)
    { _samba._writeU32( EFC0_FCR, (EFC_KEY << 24) | (arg << 8) | cmd ); }

    private void _writeFCR1(final long cmd, final long arg)
    { _samba._writeU32( EFC1_FCR, (EFC_KEY << 24) | (arg << 8) | cmd ); }

    private long _readFSR0()
    { return _samba._readU32(EFC0_FSR); }

    private long _readFSR1()
    { return _samba._readU32(EFC1_FSR); }

    private boolean _waitFSR_timeoutMS(final int timeoutMS)
    {
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(timeoutMS);

        long fsr0 = 0x00;
        long fsr1 = 0x01;

        while( !tms.timeout() ) {

                fsr0 = _readFSR0();
                if( (fsr0 & 0x08) != 0 ) { SysUtil.stdDbg().println("EFC0_FSR.PROGE"); return false; } // Program error
                if( (fsr0 & 0x04) != 0 ) { SysUtil.stdDbg().println("EFC0_FSR.LOCKE"); return false; } // Lock error

            if(_flashPlanes > 1) {
                fsr1 = _readFSR1();
                if( (fsr1 & 0x08) != 0 ) { SysUtil.stdDbg().println("EFC1_FSR.PROGE"); return false; } // Program error
                if( (fsr1 & 0x04) != 0 ) { SysUtil.stdDbg().println("EFC1_FSR.LOCKE"); return false; } // Lock error
            }

            if( (fsr0 & fsr1 & 0x01) != 0 ) return true;

        } // while

        SysUtil.stdDbg().println("EFCn_FSR : TIMEOUT");

        return false; // Timeout
    }

    private boolean _waitFSR_timeout01S() { return _waitFSR_timeoutMS( 1000); }
    private boolean _waitFSR_timeout02S() { return _waitFSR_timeoutMS( 2000); }
    private boolean _waitFSR_timeout32S() { return _waitFSR_timeoutMS(32000); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean eraseAll(final long startOffset)
    {
        // The start offset must be zero
        if(startOffset != 0) return false;

        if( !_waitFSR_timeout01S() ) return false;

                             _writeFCR0(EFC_FCMD_EA, 0              );
        if(_flashPlanes > 1) _writeFCR1(EFC_FCMD_EA, _flashPages / 2);

        return _waitFSR_timeout32S();
    }

    @Override
    public boolean writePage(final int pageNum, final boolean eraseBeforeWrite)
    {
        if(pageNum >= _flashPages) return false;

        if(true) {
           if( !_waitFSR_timeout01S() ) return false;

           long fmr0 = _samba._readU32(EFC0_FMR);
           long fmr1 = _samba._readU32(EFC1_FMR);

           if(eraseBeforeWrite) { fmr0 &= ~(1L << 7); fmr1 &= ~(1L << 7); }
           else                 { fmr0 |=  (1L << 7); fmr1 |=  (1L << 7); }

           _samba._writeU32(EFC0_FMR, fmr0);
           _samba._writeU32(EFC1_FMR, fmr1);
        }

        if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
        if( !_samba._applet_setDstAddr(_flashBase + pageNum * _flashPageSize        ) ) return false;
        _pageBufferASel = !_pageBufferASel;

        if( !_waitFSR_timeout01S() ) return false;

        if( !_samba._applet_runT1() ) return false;

        if(_flashPlanes > 1 && pageNum >= _flashPages / 2) _writeFCR1(EFC_FCMD_WP, pageNum - _flashPages / 2);
        else                                               _writeFCR0(EFC_FCMD_WP, pageNum                  );

        if( !_waitFSR_timeout02S() ) return false;

        return true;
    }

    @Override
    public boolean readPage(final int pageNum, final int[] buffer)
    {
        if(pageNum >= _flashPages) return false;

        if( !_waitFSR_timeout01S() ) return false;

        return _samba._readBuff(_flashBase + pageNum * _flashPageSize, buffer, _flashPageSize);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Add more features (security bit, custom option bits, lock regions, etc.) !!! #####

    private boolean _modOptionBit(final String desc, final long bit, final boolean set)
    {
        // Print message
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMMod("[SAM-BA]", 9, "GPNVM." + desc, set) );

        // Set the bit
        if( !_waitFSR_timeout01S() ) return false;

        _writeFCR0(set ? EFC_FCMD_SGPB : EFC_FCMD_CGPB, bit);

        if( !_waitFSR_timeout01S() ) return false;

        // Done
        return true;
    }

    private boolean _setOptionBit(final String desc, final long bit)
    { return _modOptionBit(desc, bit, true ); }

    private boolean _clrOptionBit(final String desc, final long bit)
    { return _modOptionBit(desc, bit, false); }

    public boolean writeOptions()
    {
        // NOTE : This class will never set the 'Security' bit and lock region(s)!

        // Read the GPNVM bits
        final long gpnvm = _readFSR0();

        /*
         * Parse the GPNVM bits
         *
         * SAM7S Series
         * https://ww1.microchip.com/downloads/en/DeviceDoc/doc6175.pdf
         *
         * SAM7SE Series
         * https://ww1.microchip.com/downloads/en/DeviceDoc/doc6222.pdf
         */
        final long BIT_SEC =                  4     ; // Security
        final long BIT_BOD = _canBrownout  ?  8 : -1; // Brownout Detector Enable
        final long BIT_BOR = _canBrownout  ?  9 : -1; // Brownout Reset    Enable
        final long BIT_BMS = _canBootFlash ? 10 : -1; // Boot Mode Select

        final long MSK_SEC = (BIT_SEC >= 0) ? (1 << BIT_SEC) : 0;
        final long MSK_BOD = (BIT_BOD >= 0) ? (1 << BIT_BOD) : 0;
        final long MSK_BOR = (BIT_BOR >= 0) ? (1 << BIT_BOR) : 0;
        final long MSK_BMS = (BIT_BMS >= 0) ? (1 << BIT_BMS) : 0;

        final boolean bitSEC = (gpnvm & MSK_SEC) != 0;
        final boolean bitBOD = (gpnvm & MSK_BOD) != 0;
        final boolean bitBOR = (gpnvm & MSK_BOR) != 0;
        final boolean bitBMS = (gpnvm & MSK_BMS) != 0;

        // Set the default option bits
        if(BIT_BOD >= 0 && !bitBOD) { if( !_setOptionBit("BOD", BIT_BOD) ) return false; }
        if(BIT_BOR >= 0 && !bitBOR) { if( !_setOptionBit("BOR", BIT_BOR) ) return false; }
        if(BIT_BMS >= 0 && !bitBMS) { if( !_setOptionBit("BMS", BIT_BMS) ) return false; }

        // Done
        return true;
    }

} // class FlashEFC


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
public static class FlashEEFC extends FlashOper {

    // NOTE : This class was adapted from the BOSSA source code

    private static final long EEFC0_FMR(final long base) { return base + 0x0000; }
    private static final long EEFC0_FCR(final long base) { return base + 0x0004; }
    private static final long EEFC0_FSR(final long base) { return base + 0x0008; }
    private static final long EEFC0_FRR(final long base) { return base + 0x000C; }

    private static final long EEFC1_FMR(final long base) { return base + 0x0200; }
    private static final long EEFC1_FCR(final long base) { return base + 0x0204; }
    private static final long EEFC1_FSR(final long base) { return base + 0x0208; }
    private static final long EEFC1_FRR(final long base) { return base + 0x020C; }

    private static final long  EEFC_KEY        = 0x5A;

    private static final long  EEFC_FCMD_GETD  = 0x00;
    private static final long  EEFC_FCMD_WP    = 0x01;
    private static final long  EEFC_FCMD_WPL   = 0x02;
    private static final long  EEFC_FCMD_EWP   = 0x03;
    private static final long  EEFC_FCMD_EWPL  = 0x04;
    private static final long  EEFC_FCMD_EA    = 0x05;
    private static final long  EEFC_FCMD_EPA   = 0x07;
    private static final long  EEFC_FCMD_SLB   = 0x08;
    private static final long  EEFC_FCMD_CLB   = 0x09;
    private static final long  EEFC_FCMD_GLB   = 0x0A;
    private static final long  EEFC_FCMD_SGPB  = 0x0B;
    private static final long  EEFC_FCMD_CGPB  = 0x0C;
    private static final long  EEFC_FCMD_GGPB  = 0x0D;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FlashEEFC(
        final ProgBootSAMBA instProgBootSAMBA,
        final long          flashBaseAddress,
        final int           flashNumberOfPages,
        final int           flashBytePageSize,
        final int           flashNumberOfPlanes,
        final int           flashNumberOfLockRegions,
        final long          sramAppletStartAddress,
        final long          sramAppletStackAddress,
        final long          regBase,
        final boolean       canBrownout,
        final boolean       canBootFlash
    )
    {
        // Call the superclass constructor
        super(
            instProgBootSAMBA     ,
            flashBaseAddress      , flashNumberOfPages    , flashBytePageSize, flashNumberOfPlanes, flashNumberOfLockRegions,
            sramAppletStartAddress, sramAppletStackAddress,
            regBase               ,
            canBrownout           , canBootFlash
        );

        /* SAM3 Errata (EEFCn_FMR.FWS must be 6)
         *
         * https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-11057-32-bit-Cortex-M3-Microcontroller-SAM3X-SAM3A_Datasheet.pdf
         *
         *     When writing data to the Flash memory plane the data may not be written correctly.
         *     To ensure proper programming, set the number of Wait States (WS) to 6 (i.e., FWS = 6)
         *     during the write operation.
         */
                             _samba._writeU32( EEFC0_FMR(_regBase), 0x06 << 8 );
        if(_flashPlanes > 1) _samba._writeU32( EEFC1_FMR(_regBase), 0x06 << 8 );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _waitFSR_timeoutMS(final int timeoutMS)
    {
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(timeoutMS);

        long fsr0 = 0x00;
        long fsr1 = 0x01;

        while( !tms.timeout() ) {

                fsr0 = _samba._readU32( EEFC0_FSR(_regBase) );
                if( (fsr0 & 0x02) != 0 ) { SysUtil.stdDbg().println("EEFC0_FSR.FCMDE" ); return false; } // Command error
                if( (fsr0 & 0x04) != 0 ) { SysUtil.stdDbg().println("EEFC0_FSR.FLOCKE"); return false; } // Lock error

            if(_flashPlanes > 1) {
                fsr1 = _samba._readU32( EEFC1_FSR(_regBase) );
                if( (fsr1 & 0x02) != 0 ) { SysUtil.stdDbg().println("EEFC1_FSR.FCMDE" ); return false; } // Command error
                if( (fsr1 & 0x04) != 0 ) { SysUtil.stdDbg().println("EEFC1_FSR.FLOCKE"); return false; } // Lock error
            }

            if( (fsr0 & fsr1 & 0x01) != 0 ) return true;

        } // while

        SysUtil.stdDbg().println("EEFCn_FSR : TIMEOUT");

        return false; // Timeout
    }

    private boolean _waitFSR_timeout01S() { return _waitFSR_timeoutMS( 1000); }
    private boolean _waitFSR_timeout02S() { return _waitFSR_timeoutMS( 2000); }
    private boolean _waitFSR_timeout32S() { return _waitFSR_timeoutMS(32000); }

    private void _writeFCR0(final long cmd, final long arg)
    { _samba._writeU32( EEFC0_FCR(_regBase), (EEFC_KEY << 24) | (arg << 8) | cmd ); }

    private void _writeFCR1(final long cmd, final long arg)
    { _samba._writeU32( EEFC1_FCR(_regBase), (EEFC_KEY << 24) | (arg << 8) | cmd ); }

    private long _readFRR0()
    { return _samba._readU32( EEFC0_FRR(_regBase) ); }

    private long _readFRR1()
    { return _samba._readU32( EEFC1_FRR(_regBase) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean eraseAll(final long startOffset)
    {
        // The start offset must be on an erase page boundary
        final int PagesPerErase = 8;

        if( ( startOffset % (_flashPageSize * PagesPerErase) ) != 0 ) return false;

        // Erase each PagesPerErase set of pages
        if( !_waitFSR_timeout01S() ) return false;

        final int startPage     = (int) (startOffset / _flashPageSize);
        final int pagesPerPlane = _flashPages / 2;

        for(int pageNum = startPage; pageNum < _flashPages; pageNum += PagesPerErase) {

            if(_flashPlanes == 1 || pageNum < pagesPerPlane) _writeFCR0( EEFC_FCMD_EPA, ( (pageNum                ) << 0 ) | 0x01 );
            else                                             _writeFCR1( EEFC_FCMD_EPA, ( (pageNum % pagesPerPlane) << 0 ) | 0x01 );

            if( !_waitFSR_timeout02S() ) {
                // In case of error, try EEFC_FCMD_EA if both 'startOffset' and 'pageNum' are zero
                if(startOffset == 0 && pageNum == 0) {
                                         _writeFCR0(EEFC_FCMD_EA, 0);
                    if(_flashPlanes > 1) _writeFCR1(EEFC_FCMD_EA, 0);
                    return _waitFSR_timeout32S();
                }
                else {
                    return false;
                }
            }

        } // for

        // Done
        return true;
    }

    @Override
    public boolean writePage(final int pageNum, final boolean eraseBeforeWrite)
    {
        if(pageNum >= _flashPages) return false;

        if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
        if( !_samba._applet_setDstAddr(_flashBase + pageNum * _flashPageSize        ) ) return false;
        _pageBufferASel = !_pageBufferASel;

        if( !_waitFSR_timeout01S() ) return false;

        if( !_samba._applet_runT2() ) return false;

        if(_flashPlanes > 1 && pageNum >= _flashPages / 2) _writeFCR1(eraseBeforeWrite ? EEFC_FCMD_EWP : EEFC_FCMD_WP, pageNum - _flashPages / 2);
        else                                               _writeFCR0(eraseBeforeWrite ? EEFC_FCMD_EWP : EEFC_FCMD_WP, pageNum                  );

        if( !_waitFSR_timeout02S() ) return false;

        return true;
    }

    @Override
    public boolean readPage(final int pageNum, final int[] buffer)
    {
        if(pageNum >= _flashPages) return false;

        if( !_samba._applet_setSrcAddr(_flashBase + pageNum * _flashPageSize        ) ) return false;
        if( !_samba._applet_setDstAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;

        if( !_waitFSR_timeout01S() ) return false;

        if( !_samba._applet_runT2() ) return false;

        return _samba._readBuff(_pageBufferASel ? _pageBufferA : _pageBufferB, buffer, _flashPageSize);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Add more features (security bit, custom option bits, lock regions, etc.) !!! #####

    private boolean _modOptionBit(final String desc, final long bit, final boolean set)
    {
        // Print message
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMMod("[SAM-BA]", 9, "GPNVM." + desc, set) );

        // Set the bit
        if( !_waitFSR_timeout01S() ) return false;

        _writeFCR0(set ? EEFC_FCMD_SGPB : EEFC_FCMD_CGPB, bit);

        if( !_waitFSR_timeout01S() ) return false;

        // Done
        return true;
    }

    private boolean _setOptionBit(final String desc, final long bit)
    { return _modOptionBit(desc, bit, true ); }

    private boolean _clrOptionBit(final String desc, final long bit)
    { return _modOptionBit(desc, bit, false); }

    public boolean writeOptions()
    {
        // NOTE : This class will never set the 'Security' bit and lock region(s)!

        // Read the GPNVM bits
        if( !_waitFSR_timeout01S() ) return false;
        _writeFCR0(EEFC_FCMD_GGPB, 0);
        if( !_waitFSR_timeout01S() ) return false;

        final long gpnvm = _readFRR0();

        /*
         * Parse the GPNVM bits
         *
         * SAM3X / SAM3A Series
         * https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-11057-32-bit-Cortex-M3-Microcontroller-SAM3X-SAM3A_Datasheet.pdf
         *
         * SAM9XE Series
         * https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-6254-32-bit-ARM926EJ-S-Embedded-Microprocessor-SAM9XE_Datasheet.pdf
         */
        final long BIT_SEC =                                 0          ; // Security
        final long BIT_BOD = _canBrownout  ?                 1      : -1; // Brownout Detector Enable
        final long BIT_BOR = _canBrownout  ?                 2      : -1; // Brownout Reset    Enable
        final long BIT_BMS = _canBootFlash ? (_canBrownout ? 3 : 1) : -1; // Boot Mode Select

        final long MSK_SEC = (BIT_SEC >= 0) ? (1 << BIT_SEC) : 0;
        final long MSK_BOD = (BIT_BOD >= 0) ? (1 << BIT_BOD) : 0;
        final long MSK_BOR = (BIT_BOR >= 0) ? (1 << BIT_BOR) : 0;
        final long MSK_BMS = (BIT_BMS >= 0) ? (1 << BIT_BMS) : 0;

        final boolean bitSEC = (gpnvm & MSK_SEC) != 0;
        final boolean bitBOD = (gpnvm & MSK_BOD) != 0;
        final boolean bitBOR = (gpnvm & MSK_BOR) != 0;
        final boolean bitBMS = (gpnvm & MSK_BMS) != 0;

        // Set the default option bits
        if(BIT_BOD >= 0 && !bitBOD) { if( !_setOptionBit("BOD", BIT_BOD) ) return false; }
        if(BIT_BOR >= 0 && !bitBOR) { if( !_setOptionBit("BOR", BIT_BOR) ) return false; }
        if(BIT_BMS >= 0 && !bitBMS) { if( !_setOptionBit("BMS", BIT_BMS) ) return false; }

        // Done
        return true;
    }

} // class FlashEEFC


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
private static class WordCopyARM {

    // NOTE : This class was adapted from the BOSSA source code

    private static long[] _opcode   = null;

    private static long   _start    = -1;
    private static long   _stack    = -1;
    private static long   _reset    = -1;
    private static long   _dst_addr = -1;
    private static long   _src_addr = -1;
    private static long   _words    = -1;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean build(final boolean dumpDisassemblyAndArray)
    {
        // Check if it is already built
        if(_opcode != null) return true;

        // Built the program
        try {

            // Create the program
            ARMCortexMThumb asm = new ARMCortexMThumb(CPU.M0) {{

              // start
                    $ldr  (Reg.R0   , "dst_addr");
                    $ldr  (Reg.R1   , "src_addr");
                    $ldr  (Reg.R2   , "words"   );
                    $b    ("check"              );

                label("copy");
                    $ldmia(Reg.R1.wb, Reg.R3    );
                    $stmia(Reg.R0.wb, Reg.R3    );
                    $subs (Reg.R2   , 1         );

                label("check");
                    $cmp  (Reg.R2   , 0         );
                    $bne  ("copy"               );

                    $ldr  (Reg.R0   , "reset"   );
                    $cmp  (Reg.R0   , 0         );
                    $bne  ("return"             );
                    $ldr  (Reg.R0   , "stack"   );
                    $mov  (Reg.SP   , Reg.R0    );

                label("return");
                    $bx   (Reg.LR               );

                $_align(2);

                label("stack");
                    $_word(0);
                label("reset");
                    $_word(0);
                label("dst_addr");
                    $_word(0);
                label("src_addr");
                    $_word(0);
                label("words");
                    $_word(0);

            }};

            // Link the program
            _opcode = asm.link(ProgSWD.DefaultCortexM_SRAMStart, dumpDisassemblyAndArray);

            if(dumpDisassemblyAndArray) {
                SysUtil.stdDbg().println();
                SysUtil.stdDbg().printf( "### dst_addr = 0x%08X\n", asm.resolvedLabelAddress("dst_addr") );
                SysUtil.stdDbg().printf( "### reset    = 0x%08X\n", asm.resolvedLabelAddress("reset"   ) );
                SysUtil.stdDbg().printf( "### src_addr = 0x%08X\n", asm.resolvedLabelAddress("src_addr") );
                SysUtil.stdDbg().printf( "### stack    = 0x%08X\n", asm.resolvedLabelAddress("stack"   ) );
                SysUtil.stdDbg().printf( "### start    = 0x%08X\n", 0                                    );
                SysUtil.stdDbg().printf( "### words    = 0x%08X\n", asm.resolvedLabelAddress("words"   ) );
                SysUtil.stdDbg().println();
            }

            // Save the addresses
            _dst_addr = asm.resolvedLabelAddress("dst_addr");
            _reset    = asm.resolvedLabelAddress("reset"   );
            _src_addr = asm.resolvedLabelAddress("src_addr");
            _stack    = asm.resolvedLabelAddress("stack"   );
            _start    = 0;
            _words    = asm.resolvedLabelAddress("words"   );

            // Done
            return true;

        }
        catch(final JXMAsmError e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static long[] opcode  () { return _opcode;            }
    public static long   size    () { return _opcode.length * 4; } // In bytes

    public static long   start   () { return _start;             }
    public static long   stack   () { return _stack;             }
    public static long   reset   () { return _reset;             }
    public static long   dst_addr() { return _dst_addr;          }
    public static long   src_addr() { return _src_addr;          }
    public static long   words   () { return _words;             }

} // class WordCopyARM


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : Some SAM-BA implementations may return LF+CR instead of the expected CR+LF

    private static final byte[] _crlf = new byte[] { '\r', '\n' };
    private static final byte[] _lfcr = new byte[] { '\n', '\r' };

    private boolean _sambaRx(final int[] buff, final int size)
    { return _serialRxUntil(buff, size, _crlf, _lfcr); }

    private int[] _sambaRx(final int size)
    {
        final int[] buff = new int[size];

        if( !_sambaRx(buff, size) ) return null;

        return buff;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _smbSwitchToInteractiveMode()
    {
        /*
         * Command                    : T#
         * Response (    Interactive) : CRLF + CRLF + prompt
         *          (Non-interactive) : CRLF + prompt
         */

         // Send the command
        _pbs_wrBuff_32I[0] = 'T';
        _pbs_wrBuff_32I[1] = '#';

        _serialTx(_pbs_wrBuff_32I, 2);

        // Check for one CRLF and at least one more character (the prompt)
        if( !_sambaRx(_pbs_wrBuff_32I, 1) ) return false;

        // Done
        _serialDrain();
        return true;
    }

    private boolean _smbSwitchToNonInteractiveMode()
    {
        // NOTE : This function assumes the bootloader is currently in interactive mode

        /*
         * Command                    : N#
         * Response (    Interactive) : CRLF
         *          (Non-interactive) : <nothing>
         */

         // Send the command
        _pbs_wrBuff_32I[0] = 'N';
        _pbs_wrBuff_32I[1] = '#';

        _serialTx(_pbs_wrBuff_32I, 2);

        // Check for one CRLF only
        return _sambaRx(_pbs_wrBuff_32I, 0);
    }

    private boolean _smbConnect()
    {
        // Send the auto-baud sequence (it may be required by some implementations)
        _pbs_wrBuff_32I[0] = 0x80;
        _pbs_wrBuff_32I[1] = 0x80;
        _pbs_wrBuff_32I[2] = '#';

        _serialTx(_pbs_wrBuff_32I, 3);
        _serialDrain();

        /*
         * Attempt to switch to non-interactive (binary) mode
         *
         * NOTE : Some SAM-BA implementations do not support one or both of these commands
         */
         if(true) {
            // Prevent error messages from being printed to the console
            USB2GPIO.redirectNotifyErrorToString(true);
            // Perform the switch
            boolean error = false;
            if( _smbSwitchToInteractiveMode() ) {
                // If the above command is supported, this one should be as well; execute it
                // and verify the result
                error = !_smbSwitchToNonInteractiveMode();
            }
            else {
                // If the above command is not supported, this one might still be required or
                // might also be unsupported; execute it regardless and ignore the result.
                _setSerialRxTimeout_MS_minimum();
                _smbSwitchToNonInteractiveMode();
                _setSerialRxTimeout_MS_default();
            }
            // Allow error messages to be printed to the console
            USB2GPIO.redirectNotifyErrorToString(false);
            // Check for error
            if(error) return false;
        }

        // Get the version number
        _pbs_wrBuff_32I[0] = 'V';
        _pbs_wrBuff_32I[1] = '#';

        _serialTx(_pbs_wrBuff_32I, 2);

        final int[] verBytes = _sambaRx(256);

        if(verBytes == null) return false;

        final String raw = new String( USB2GPIO.ia2ba(verBytes), StandardCharsets.US_ASCII );
        final String ver = raw.substring( 0, raw.indexOf('\0') );

        SysUtil.stdDbg().printf("\n[SAM-BA] %s\n", ver);

        // Determine if the SAM-BA will use XMODEM for 'R' and 'S' command
        if(true) {
            // Prevent error messages from being printed to the console
            USB2GPIO.redirectNotifyErrorToString(true);
            // Send an 'R' request and heuristically evaluate if the response resembles an XMODEM frame
            final byte[] cmdR8 = String.format( "R%08X,%08X#C", _configAppletInfo().sramAppletStart, 8 ).getBytes(StandardCharsets.US_ASCII);
            if( _serialTx(cmdR8) ) {
                // Receive (128 + 5) bytes (the size of XMODEM frame)
                final int[] recv = _serialRx(128 + 5);
                // Check if (128 + 5) bytes are received
                if(recv != null) {
                    // Send ACK, delay, and send ACK again
                    _serialTx( new byte[] { 0x06 } );
                    SysUtil.sleepMS(10);
                    _serialTx( new byte[] { 0x06 } );
                    // Check the first 3 bytes
                    if( recv[0] == 0x01 && recv[1] == ( (~recv[2]) & 0xFF ) ) {
                        // Instantiate the XModem helper class
                        _xmodem = new XModem(_ttyPort);
                        SysUtil.stdDbg().printf("[SAM-BA] [XMODEM]\n\n");
                    }
                }
                // Clear the serial port buffers
                _serialFlush();
                _serialDrain();
            }
            // Allow error messages to be printed to the console
            USB2GPIO.redirectNotifyErrorToString(false);
        }

        if(_xmodem == null)  SysUtil.stdDbg().println();

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * All SAMD-based Arduino and Adafruit boards have a bootloader bug where reading 64 bytes or
     * more over USB results in data corruption.
     */
    static final int MaxBulkReadSize = 63;

    private static int _putUxxHex_impl(final int[] buffer, final int ofs, final long value, final int digits)
    {
        for(int i = 0; i < digits; ++i) {
            final int nibble = (int) ( ( value >> ( (digits - 1 - i) * 4) ) & 0x0F );
            buffer[ofs + i] = ( (nibble < 10) ? ('0' + nibble) : ('A' + nibble - 10) );
        }

        return ofs + digits;
    }

    private static int _putU08Hex(final int[] buffer, final int ofs, final long value)
    { return _putUxxHex_impl(buffer, ofs, value, 2); }

    private static int _putU16Hex(final int[] buffer, final int ofs, final long value)
    { return _putUxxHex_impl(buffer, ofs, value, 4); }

    private static int _putU32Hex(final int[] buffer, final int ofs, final long value)
    { return _putUxxHex_impl(buffer, ofs, value, 8); }

    public int _readU08(final long address)
    {
        /*
         * Command                    : o<8 hex digits>,1#
         * Response (Non-interactive) : <1 byte>
         */

        // Send the command
        int idx = 0;

        _pbs_wrBuff_32I[idx++] = 'o';

        idx = _putU32Hex( _pbs_wrBuff_32I, idx, address );
                          _pbs_wrBuff_32I[idx++] = ','   ;
                          _pbs_wrBuff_32I[idx++] = '1'   ;
                          _pbs_wrBuff_32I[idx++] = '#'   ;

        _serialTx(_pbs_wrBuff_32I, idx);

        // Read and return the response
        final Integer res = _serialRxUInt8();

        return (res != null) ? res : -1;
    }

    public void _writeU08(final long address, final int value)
    {
        /*
         * Command                    : O<8 hex digits>,<2 hex digits>#
         * Response (Non-interactive) : <nothing>
         */

        // Send the command
        int idx = 0;

                          _pbs_wrBuff_32I[idx++] = 'O'   ;
        idx = _putU32Hex( _pbs_wrBuff_32I, idx, address );
                          _pbs_wrBuff_32I[idx++] = ','   ;
        idx = _putU08Hex( _pbs_wrBuff_32I, idx, value   );
                          _pbs_wrBuff_32I[idx++] = '#'   ;

        _serialTx(_pbs_wrBuff_32I, idx);

        // Done
        _serialFlush();
    }

    public int _readU16(final long address)
    {
        /*
         * Command                    : h<8 hex digits>,2#
         * Response (Non-interactive) : <4 bytes LE>
         */

        // Send the command
        int idx = 0;

        _pbs_wrBuff_32I[idx++] = 'h';

        idx = _putU32Hex( _pbs_wrBuff_32I, idx, address );
                          _pbs_wrBuff_32I[idx++] = ','   ;
                          _pbs_wrBuff_32I[idx++] = '2'   ;
                          _pbs_wrBuff_32I[idx++] = '#'   ;

        _serialTx(_pbs_wrBuff_32I, idx);

        // Read and return the response
        final Integer res = _serialRxUInt16LE();

        return (res != null) ? res : -1;
    }

    public void _writeU16(final long address, final long value)
    {
        /*
         * Command                    : H<8 hex digits>,<4 hex digits>#
         * Response (Non-interactive) : <nothing>
         */

        // Send the command
        int idx = 0;

                          _pbs_wrBuff_32I[idx++] = 'H'   ;
        idx = _putU32Hex( _pbs_wrBuff_32I, idx, address );
                          _pbs_wrBuff_32I[idx++] = ','   ;
        idx = _putU16Hex( _pbs_wrBuff_32I, idx, value   );
                          _pbs_wrBuff_32I[idx++] = '#'   ;

        _serialTx(_pbs_wrBuff_32I, idx);

        // Done
        _serialFlush();
    }

    public long _readU32(final long address)
    {
        /*
         * Command                    : w<8 hex digits>,4#
         * Response (Non-interactive) : <4 bytes LE>
         */

        // Send the command
        int idx = 0;

        _pbs_wrBuff_32I[idx++] = 'w';

        idx = _putU32Hex( _pbs_wrBuff_32I, idx, address );
                          _pbs_wrBuff_32I[idx++] = ','   ;
                          _pbs_wrBuff_32I[idx++] = '4'   ;
                          _pbs_wrBuff_32I[idx++] = '#'   ;

        _serialTx(_pbs_wrBuff_32I, idx);

        // Read and return the response
        final Long res = _serialRxUInt32LE();

        return (res != null) ? res : -1;
    }

    public void _writeU32(final long address, final long value)
    {
        /*
         * Command                    : W<8 hex digits>,<8 hex digits>#
         * Response (Non-interactive) : <nothing>
         */

        // Send the command
        int idx = 0;

                          _pbs_wrBuff_32I[idx++] = 'W'   ;
        idx = _putU32Hex( _pbs_wrBuff_32I, idx, address );
                          _pbs_wrBuff_32I[idx++] = ','   ;
        idx = _putU32Hex( _pbs_wrBuff_32I, idx, value   );
                          _pbs_wrBuff_32I[idx++] = '#'   ;

        _serialTx(_pbs_wrBuff_32I, idx);

        // Done
        _serialFlush();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean _writeBuff(final long address, final int[] buffer, final int length)
    {
        /*
         * Command                    : S<8 hex digits>,<8 hex digits>#
         * Response (Non-interactive) : <nothing>
         */

        // Send the command
        int idx = 0;

                          _pbs_wrBuff_32I[idx++] = 'S'   ;
        idx = _putU32Hex( _pbs_wrBuff_32I, idx, address );
                          _pbs_wrBuff_32I[idx++] = ','   ;
        idx = _putU32Hex( _pbs_wrBuff_32I, idx, length  );
                          _pbs_wrBuff_32I[idx++] = '#'   ;

        _serialTx(_pbs_wrBuff_32I, idx);
        _serialFlush();

        // Send the data
        if(_xmodem != null) {
            if( !_xmodem.transmit(buffer, length) ) return false;
        }
        else {
            _serialTx(buffer, length);
        }
        _serialFlush();

        // Done
        return true;
    }

    public boolean _writeBuff(final long address, final int[] buffer)
    { return _writeBuff(address, buffer, buffer.length); }

    public boolean _readBuff(final long address, final int[] buffer, final int length)
    {
        /*
         * Command                    : R<8 hex digits>,<8 hex digits>#
         * Response (Non-interactive) : <1 byte>, ...
         */

        /*
         * The SAM firmware has a known issue when reading power-of-two lengths greater than 32 bytes via USB.
         * If this condition applies, read the first byte separately, then read the remaining (length - 1) bytes afterward.
         */
        long adr = address;
        int  len = length;
        int  pos = 0;

        if( len > 32 && XCom.isPowerOfTwo(len) ) {
            final int b1 = _readU08(adr);
            if(b1 < 0) return false;
            buffer[pos++] = b1;
            ++adr;
            --len;
        }

        // Read the remining bytes
        int[] cbytes = null;

        while(len > 0) {

            // Prepare the buffer
            final int ChunkSize = Math.min(MaxBulkReadSize, len);

            if(cbytes == null || cbytes.length != ChunkSize) cbytes = new int[ChunkSize];

            // Send the command
            int idx = 0;

                              _pbs_wrBuff_32I[idx++] = 'R'     ;
            idx = _putU32Hex( _pbs_wrBuff_32I, idx, adr       );
                              _pbs_wrBuff_32I[idx++] = ','     ;
            idx = _putU32Hex( _pbs_wrBuff_32I, idx, ChunkSize );
                              _pbs_wrBuff_32I[idx++] = '#'     ;

            _serialTx(_pbs_wrBuff_32I, idx);
            _serialFlush();

            // Receive the data
            if(_xmodem != null) {
                if( !_xmodem.receive(cbytes, ChunkSize) ) return false;
            }
            else {
                if( !_serialRx(cbytes, ChunkSize) ) return false;
            }

            // Copy the data
            for(final int bn : cbytes) buffer[pos++] = bn;

            // Update the variables
            adr += ChunkSize;
            len -= ChunkSize;

        } // while

        // Done
        return true;
    }

    public boolean _readBuff(final long address, final int[] buffer)
    { return _readBuff(address, buffer, buffer.length); }

    public void _go(final long address)
    {
        /*
         * Command                    : G<8 hex digits>#
         * Response (Non-interactive) : <nothing>
         */

        // Send the command
        int idx = 0;

                          _pbs_wrBuff_32I[idx++] = 'G'   ;
        idx = _putU32Hex( _pbs_wrBuff_32I, idx, address );
                          _pbs_wrBuff_32I[idx++] = '#'   ;

        _serialTx(_pbs_wrBuff_32I, idx);

        // Done
        _serialFlush();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static long SMB_MASK_CHIP_ID  = 0x7FFFFFE0L; // 01111111 11111111 11111111 11100000 (chip   ID mask to exclude the EXT     and VERSION bits )
    private static long SMB_MASK_ECHIP_ID = 0xFFFFFFFFL; // 11111111 11111111 11111111 11111111
    private static long SMB_MASK_DEV_ID   = 0xFFFF00FFL; // 11111111 11111111 00000000 11111111 (device ID mask to exclude the DIE     and REVISION bits)
    private static long SMB_MASK_CPU_ID   = 0xFF0FFFF0L; // 11111111 00001111 11111111 11110000 (CPU    ID mask to exclude the VARIANT and REVISION bits)

    private long _smb_chipID  = -1;
    private long _smb_echipID = -1;
    private long _smb_cpuID   = -1;
    private long _smb_devID   = -1;

    private boolean _smbReadExtChipID()
    {
        // NOTE : This function was adapted from the BOSSA source code

        _smb_chipID = _readU32(0x400E0740L);
        if(_smb_chipID != 0) {
            if(_smb_chipID < 0) return false;
            _smb_echipID = _readU32(0x400E0744L);
            return true;
        }

        _smb_chipID = _readU32(0x400E0940L);
        if(_smb_chipID != 0) {
            if(_smb_chipID < 0) return false;
            _smb_echipID = _readU32(0x400E0944L);
            return true;
        }

        return false;
    }

    private boolean _smbReadIDs()
    {
        // NOTE : This function was adapted from the BOSSA source code

        /*
         * Read instruction at reset vector (0x0) and check if it is an unconditional ARM branch (opcode 0xEAxxxxxx).
         * If so, treat the device as ARM7TDMI (ARMv4T) and probe Atmel SAM7/9 with CHIPID at 0xFFFFF240.
         */
        final long opcode0 = _readU32(0x00000000L);

        if(opcode0 < 0) return false;

        if( (opcode0 & 0xFF000000L) == 0xEA000000L ) {
            _smb_chipID = _readU32(0xFFFFF240L);
            if(_smb_chipID < 0) return false;
        }

        // Otherwise, check the ARM CPUID register, as it is supported by all Cortex-M MCUs
        else {
            // Read the CPUID
            _smb_cpuID = _readU32(0xE000ED00L);
            if(_smb_cpuID < 0) return false;
            // Mask the CPUID to retain only the PARTNO bits
            final long _masked_smb_cpuID = _smb_cpuID & 0x0000FFF0L;
            // Check for Cortex-M0+
            if(_masked_smb_cpuID == 0x0000C600L) {
                _smb_devID = _readU32(0x41002018L);
                if(_smb_devID < 0) return false;
            }
            // Check for Cortex-M4
            else if(_masked_smb_cpuID == 0x0000C240L) {
                // Read the reset vector at 0x4 and check if it points to the SAM-BA ROM (for SAM4 MCU)
                final long opcode4 = _readU32(0x00000004L);
                if(opcode4 < 0) return false;
                if( (opcode4 & 0xFFF00000L) == 0x00800000L ) {
                    // Read the SAM chip ID registers
                    if( !_smbReadExtChipID() ) return false;
                }
                else {
                    // Read the SAMx device ID register
                    _smb_devID = _readU32(0x41002018L);
                    if(_smb_devID < 0) return false;
                }
            }
            // Other Cortex MCUs
            else {
                // Read the Atmel chip ID registers
                if( !_smbReadExtChipID() ) return false;
            }
        }

        // Mask the IDs
        _smb_chipID  = (_smb_chipID  < 0) ? 0 : (_smb_chipID  & SMB_MASK_CHIP_ID );
        _smb_echipID = (_smb_echipID < 0) ? 0 : (_smb_echipID & SMB_MASK_ECHIP_ID);
        _smb_devID   = (_smb_devID   < 0) ? 0 : (_smb_devID   & SMB_MASK_DEV_ID  );
        _smb_cpuID   = (_smb_cpuID   < 0) ? 0 : (_smb_cpuID   & SMB_MASK_CPU_ID  );

        // Use one of the IDs as the signature
        if(_mcuSignature == null &&  _configMCUInfo().chipID != null && _smb_chipID > 0) {
            _mcuSignature = new int[] {
                (int) ( (_smb_chipID >> 24) & 0xFF ),
                (int) ( (_smb_chipID >> 16) & 0xFF ),
                (int) ( (_smb_chipID >>  8) & 0xFF ),
                (int) ( (_smb_chipID >>  0) & 0xFF )
            };
        }

        if(_mcuSignature == null &&  _configMCUInfo().deviceID != null && _smb_devID > 0) {
            _mcuSignature = new int[] {
                (int) ( (_smb_devID >> 24) & 0xFF ),
                (int) ( (_smb_devID >> 16) & 0xFF ),
                (int) ( (_smb_devID >>  8) & 0xFF ),
                (int) ( (_smb_devID >>  0) & 0xFF )
            };
        }

        if(_mcuSignature == null && _smb_cpuID > 0) {
            _mcuSignature = new int[] {
                (int) ( (_smb_cpuID >> 24) & 0xFF ),
                (int) ( (_smb_cpuID >> 16) & 0xFF ),
                (int) ( (_smb_cpuID >>  8) & 0xFF ),
                (int) ( (_smb_cpuID >>  0) & 0xFF )
            };
        }

        // Done
        return true;
    }

    private boolean _smbIdentify()
    {
        // NOTE : This function was adapted from the BOSSA source code

        // Read the ID(s)
        if( !_smbReadIDs() ) return false;

        // Check the ID(s)
        if( _configMCUInfo().chipID != null && !LongStream.of( _configMCUInfo().chipID ).anyMatch( x -> x == _smb_chipID ) ) {
            return USB2GPIO.notifyError(Texts.ProgXXX_FailSAMBAUnsupMCU, ProgClassName, "CHIP_ID", _smb_chipID);
        }

        if( _configMCUInfo().extChipID != null && !LongStream.of( _configMCUInfo().extChipID ).anyMatch( x -> x == _smb_echipID ) ) {
            return USB2GPIO.notifyError(Texts.ProgXXX_FailSAMBAUnsupMCU, ProgClassName, "EXT_CHIP_ID", _smb_echipID);
        }

        if( _configMCUInfo().deviceID != null && !LongStream.of( _configMCUInfo().deviceID ).anyMatch( x -> x == _smb_devID ) ) {
            return USB2GPIO.notifyError(Texts.ProgXXX_FailSAMBAUnsupMCU, ProgClassName, "DEVICE_ID", _smb_devID);
        }

        // Done
        return true;
    }

    private void _smbResetChip()
    {
        if( _configAppletInfo().regReset        == 0 ||
            _configAppletInfo().regResetCommand == 0 ) return; // Resetting the chip via SAM-BA is not supported

        _writeU32( _configAppletInfo().regReset, _configAppletInfo().regResetCommand );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean _applet_setCode(final long[] opcode)
    {
        // Convert to bytes
        final int[] bytes = new int[opcode.length * 4];
              int   idx   = 0;

        for(final long l : opcode) {
            bytes[idx++] = (int) ( (l >>  0) & 0xFF );
            bytes[idx++] = (int) ( (l >>  8) & 0xFF );
            bytes[idx++] = (int) ( (l >> 16) & 0xFF );
            bytes[idx++] = (int) ( (l >> 24) & 0xFF );
        }

        // Write the bytes
        return _writeBuff( _configAppletInfo().sramAppletStart, bytes );
    }

    public boolean _applet_setStack(final long stack)
    {
        _writeU32( _configAppletInfo().sramAppletStart + WordCopyARM.stack(), stack );
        return true;
    }

    public boolean _applet_setDstAddr(final long dstAddr)
    {
        _writeU32( _configAppletInfo().sramAppletStart + WordCopyARM.dst_addr(), dstAddr );
        return true;
    }

    public boolean _applet_setSrcAddr(final long srcAddr)
    {
        _writeU32( _configAppletInfo().sramAppletStart + WordCopyARM.src_addr(), srcAddr );
        return true;
    }

    public boolean _applet_setWords(final long words)
    {
        _writeU32( _configAppletInfo().sramAppletStart + WordCopyARM.words(), words );
        return true;
    }

    public boolean _applet_runT1() // Thumb-1 devices
    {
        _go( _configAppletInfo().sramAppletStart + WordCopyARM.start() + 1 );
        return true;
    }

    public boolean _applet_runT2() // Thumb-2 devices
    {

        _writeU32( _configAppletInfo().sramAppletStart + WordCopyARM.reset(), _configAppletInfo().sramAppletStart + WordCopyARM.start() + 1 );
        _go( _configAppletInfo().sramAppletStart + WordCopyARM.stack() );
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean   _inProgMode = false;
    private boolean   _chipErased = false;

    private XModem    _xmodem     = null;
    private FlashOper _flashOper  = null;

    @SuppressWarnings("this-escape")
    public ProgBootSAMBA(final ProgBootSAMBA.Config config) throws Exception
    {
        // Process the superclass
        super(ProgClassName, config);

        // Check the configuration values
        if(_config.memoryEEPROM.totalSize > 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMETotSize, ProgClassName);

        if( _configAppletInfo().flashDriver != null ) {
             final String trimmed = _configAppletInfo().flashDriver.trim();
             _configAppletInfo().flashDriver = trimmed.isEmpty() ? null : trimmed;
        }

        if( _configAppletInfo().flashDriver      == null ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvAIFlDriver, ProgClassName);
        if( _configAppletInfo().flashBase        <  0    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvAIFlBase  , ProgClassName);
        if( _configAppletInfo().flashNumPlanes   <= 0    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvAIFlNPlane, ProgClassName);
        if( _configAppletInfo().flashNumLockRegs <= 0    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvAIFlNLcReg, ProgClassName);

        if( _configAppletInfo().sramAppletStart  <= 0    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvAISApStart, ProgClassName);
        if( _configAppletInfo().sramAppletStack  <= 0    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvAISApStack, ProgClassName);

        if( _configAppletInfo().regBase          <  0    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvAIRegBase , ProgClassName);
        if( _configAppletInfo().regReset         <  0    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvAIRegRst  , ProgClassName);
        if( _configAppletInfo().regResetCommand  <  0    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvAIRegRstCm, ProgClassName);
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

    private boolean _begin_impl(final String serialDevice, final int baudrate, final int magicBaudrateToResetToBootloaderMode)
    {
        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Clear flag
        _chipErased = false;

        // Try to reset the MCU attached to the serial port to bootloader mode by using the magic baudrate
        //*
        if(magicBaudrateToResetToBootloaderMode > 0) {
            try {
                SerialPortUtil.tryResetMCUToBootloader_usingMagicBaudrate(serialDevice, magicBaudrateToResetToBootloaderMode);
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                return false;
            }
        }

        // Open the serial port
        if( !_openSerialPort(serialDevice, baudrate, null) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSerDev, ProgClassName);

        // Clear the serial port buffers
        _serialFlush();
        _serialDrain();
        //*/

        /*
        // Open the serial port
        // WARNING : # Using this code to initiate an erase or reset to bootloader will fail because the
        //             serial device will not re-enumerate.
        //           # On SAM devices with UART-based SAM-BA, communication is handled via a USB-to-serial
        //             converter, so USB re-enumeration does not occur.
        if( !_openSerialPort(serialDevice, baudrate, new Function<ProgBootSerial, Integer>() {
            @Override
            public Integer apply(final ProgBootSerial pbs)
            {
                // There is no need to go further if the user does not supply the magic baudrate
                if(magicBaudrateToResetToBootloaderMode <= 0) return 0;
                // Check if we can get the version number
                _pbs_wrBuff_32I[0] = 'V';
                _pbs_wrBuff_32I[1] = '#';
                _serialTx(_pbs_wrBuff_32I, 2);
                final int[] verBytes = _sambaRx(256);
                _serialFlush();
                // Return the magic baudrate as needed
                return (verBytes != null) ? 0 : magicBaudrateToResetToBootloaderMode;
            }
        } ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSerDev, ProgClassName);
        //*/

        // Build the applet (multiple calls are safe - it guards against reinitialization)
        if( !WordCopyARM.build(false) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailSAMBAAplBuild, ProgClassName);

        // Instantiate the flash operation class - must be done after 'WordCopyARM.build()'
        try {
            // Get the class name
            String clazzName = _configAppletInfo().flashDriver;
            if( !clazzName.contains(".") ) clazzName = "jxm.ugc.ProgBootSAMBA$Flash" + clazzName;
            // Get the class and constructor
            final Class<?>       clazz       = Class.forName(clazzName);
            final Constructor<?> constructor = clazz.getConstructor(ProgBootSAMBA.class, long.class, int.class, int.class, int.class, int.class, long.class, long.class, long.class, boolean.class, boolean.class);
            // Instantiate the class
            _flashOper = (FlashOper) constructor.newInstance(
                this,
                _configAppletInfo().flashBase,
                _config.memoryFlash.numPages,
                _config.memoryFlash.pageSize,
                _configAppletInfo().flashNumPlanes,
                _configAppletInfo().flashNumLockRegs,
                _configAppletInfo().sramAppletStart,
                _configAppletInfo().sramAppletStack,
                _configAppletInfo().regBase,
                _configAppletInfo().canBrownout,
                _configAppletInfo().canBootFlash
            );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return false;
        }

        // Initialize the programmer
        if( !_smbConnect () ) return false;
        if( !_smbIdentify() ) return false;

        // Initialize the applet
        if( !_applet_setCode ( WordCopyARM.opcode()          ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailSAMBAAplInit, ProgClassName);
        if( !_applet_setStack( _flashOper._appletStack       ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailSAMBAAplInit, ProgClassName);
        if( !_applet_setWords( _flashOper._flashPageSize / 4 ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailSAMBAAplInit, ProgClassName);

        // Print the IDs
        SysUtil.stdDbg().printf("[SAM-BA] CHIPID.CIDR  [MASK: 0x%08X] = 0x%08X\n", SMB_MASK_CHIP_ID , _smb_chipID );
        SysUtil.stdDbg().printf("[SAM-BA] CHIPID.EXID  [MASK: 0x%08X] = 0x%08X\n", SMB_MASK_ECHIP_ID, _smb_echipID);
        SysUtil.stdDbg().printf("[SAM-BA] DSU   .DID   [MASK: 0x%08X] = 0x%08X\n", SMB_MASK_DEV_ID  , _smb_devID  );
        SysUtil.stdDbg().printf("[SAM-BA] SCB   .CPUID [MASK: 0x%08X] = 0x%08X\n", SMB_MASK_CPU_ID  , _smb_cpuID  );
        SysUtil.stdDbg().println();

        // Set flag
        _inProgMode = true;

        // Done
        return true;
    }

    public boolean begin(final String serialDevice, final int baudrate, final int magicBaudrateToResetToBootloaderMode)
    {
        if( _begin_impl(serialDevice, baudrate, magicBaudrateToResetToBootloaderMode) ) return true;

        _closeSerialPort();

        return USB2GPIO.notifyError(Texts.ProgXXX_FailInitPrgDev, ProgClassName);
    }

    public boolean begin(final String serialDevice, final int baudrate)
    { return begin(serialDevice, baudrate, DefMagicBaudrate); }

    @Override
    public boolean end()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Write options
        // ##### !!! TODO : Only write options if no previous error has occurred? !!! #####
        if( !_flashOper.writeOptions() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

        // Attempts chip reset; may fail on certain MCUs or board designs
        _smbResetChip();

        // Close the serial port
        _closeSerialPort();

        // Reset all data
        _smb_chipID  = -1;
        _smb_echipID = -1;
        _smb_cpuID   = -1;
        _smb_devID   = -1;

        _xmodem      = null;
        _flashOper   = null;

        // Clear flag
        _inProgMode = false;

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

    // NOTE : Under the SAM-BA protocol, this function is expected to erase only flash memory
    @Override
    public boolean chipErase()
    {
        /*
         * ##### !!! WARNING !!! #####
         *
         * If the SAM-BA bootloader resides in flash rather than ROM, ensure that:
         *     Config.appletInfo.flashBLStart
         *     Config.appletInfo.flashBLSize
         * contain the correct values to prevent SAM-BA from accidentally erasing itself.
         */

        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_chipErased) return true;

        // Erase all
        if( !_flashOper.eraseAll(0) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

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
        final int  nb  = (numBytes < 0) ? _config.memoryFlash.totalSize : numBytes;
        final long la  = ( (long) startAddress & 0xFFFFFFFFL );
              int  sa  = ( la >= _configAppletInfo().flashBase ) ? ( (int) (la - _configAppletInfo().flashBase) ) : startAddress;
              int  ns  = sa % _config.memoryFlash.pageSize;
                   sa -= ns;

        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_even(sa, nb, _config.memoryFlash.totalSize, ProgClassName) ) return -1;

        // Prepare the result buffer
        if(_config.memoryFlash.readDataBuff == null || _config.memoryFlash.readDataBuff.length != numBytes) {
            _config.memoryFlash.readDataBuff = new int[numBytes];
        }

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Determine the number of pages
        final boolean notAligned = (numBytes % _config.memoryFlash.pageSize) != 0;
        final int     numPages   = (numBytes / _config.memoryFlash.pageSize) + (notAligned ? 1 : 0);
        final int     ofsPage    = sa / _config.memoryFlash.pageSize;

        // Read the bytes (and compare them if requested)
        final int[] cbytes = new int[_config.memoryFlash.pageSize];

              int   rdbIdx = 0;
              int   verIdx = 0;

        for(int c = 0; c < numPages; ++c) {

            // Read in chunk
            final int numReads = Math.min(_config.memoryFlash.pageSize, numBytes - rdbIdx);

            if( !_flashOper.readPage(ofsPage + c, cbytes) ) {
                USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);
                return -1;
            }

            // Process the chunk bytes
            for(int b = 0; b < numReads; b += 2) {

                if(ns != 0) {
                    ns -= 2;
                    continue;
                }

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
        final int ofsPage  = sa / _config.memoryFlash.pageSize;
              int cpgAddr  = sa;

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Save and clear flag
        final boolean eraseBeforeWrite = !_chipErased;

        _chipErased = false;

        // Write the pages
        int datIdx = 0;

        for(int p = 0; p < numPages; ++p) {

            // ##### !!! TODO : Skip writing the page if it is blank (its contents are all 'FlashMemory_EmptyValue') !!! #####

            // Write the page
            if( !_flashOper.loadBuffer( USB2GPIO.ba2ia(data, datIdx, _config.memoryFlash.pageSize) ) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

            if( !_flashOper.writePage(ofsPage + p, eraseBeforeWrite) ) return USB2GPIO.notifyError(Texts.ProgXXX_FailBLPrgCmdXErr, ProgClassName);

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
        final int  nb = (numBytes < 0) ? _config.memoryFlash.totalSize : numBytes;
        final long la = ( (long) startAddress & 0xFFFFFFFFL );
        final int  sa = ( la >= _configAppletInfo().flashBase ) ? ( (int) (la - _configAppletInfo().flashBase) ) : startAddress;

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
    { return _verifyReadFlash_impl(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : All SAM MCUs does not have embedded EEPROM!

    @Override
    public int readEEPROM(final int address)
    { return -1; }

    @Override
    public boolean writeEEPROM(final int address, final byte data)
    { return false; }

} // class ProgBootSAMBA
