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
import jxm.annotation.*;
import jxm.tool.*;
import jxm.ugc.fl.*;
import jxm.xb.*;


/*
 * Please refer to the comment block before the 'ProgSWD' class definition in the 'ProgSWD.java' file for more details and information.
 */
public abstract class ProgSWDLowLevel implements IProgCommon {

    protected static final String ProgClassName = "ProgSWD";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Make this configurable via 'SWDFlashLoader.Specifier' ??? #####
    public static final boolean USE_MULTI_CMD_FOR_WR_CORE_MEM = true;
    public static final boolean USE_MULTI_CMD_FOR_RD_CORE_MEM = true;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final long DefaultCortexM_SRAMStart         = 0x20000000L; // Most (all?) ARM Cortex-M MCUs have SRAM at this address
    public static final long DefaultCortexM_LoaderProgramSize = 0x00000400L; // Try to limit the size of the loader program and its stack to <= 1kB

    @SuppressWarnings("serial")
    public static class Config extends SerializableDeepClone<Config> {

        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        // NOTE : The default values below are for (almost all?) ARM Cortex-M MCUs that can be programmed using SWD

        public static class MemBankSpec  implements Serializable {
                              public long   numBanks          = 0;
            @DataFormat.Hex08 public long[] address           = null;
                              public int [] size              = null;
            @DataFormat.Hex08 public long   contiguousAddress = -1;
        }

        public static class CortexMReg implements Serializable {
            @DataFormat.Hex08 public long CPUID = 0xE000ED00L; // CPUID Base                              Register
            @DataFormat.Hex08 public long VTOR  = 0xE000ED08L; // Vector Table Offset                     Register
            @DataFormat.Hex08 public long AIRCR = 0xE000ED0CL; // Application Interrupt and Reset Control Register
            @DataFormat.Hex08 public long DFSR  = 0xE000ED30L; // Debug Fault Status                      Register
            @DataFormat.Hex08 public long DHCSR = 0xE000EDF0L; // Debug Halting Control and Status        Register
            @DataFormat.Hex08 public long DCRSR = 0xE000EDF4L; // Debug Core Register Selector            Register
            @DataFormat.Hex08 public long DCRDR = 0xE000EDF8L; // Debug Core Register Data                Register
            @DataFormat.Hex08 public long DEMCR = 0xE000EDFCL; // Debug Exception and Monitor Control     Register
            @DataFormat.Hex08 public long DSCSR = 0xE000EE08L; // Debug Security Control and Status       Register (from ARMv8-M only)
        }

        public static class MemorySRAM implements Serializable {
            // WARNING : Not all drivers can use SRAM addresses other than 'DefaultCortexM_SRAMStart'!
            @DataFormat.Hex08 public long        address     = 0;
                              public int         totalSize   = 0;

                              public MemBankSpec memBankSpec = new MemBankSpec(); // Optional; only needs to be specified for some MCUs
        }

        public static class MemoryFlash implements Serializable {
                              public String      driverName          = null;

                              public boolean     dualBank            = false;
                              public boolean     wrHalfPage          = false; // ##### ??? TODO : Hard code it to 'SWDFlashLoader.Specifier' ??? #####
            @DataFormat.Hex08 public long        address             = 0;
                              public int         totalSize           = 0;
                              public int         pageSize            = 0;
                              public int         numPages            = 0;

            @DataFormat.Hex08 public long        partEraseAddressBeg = -1;
                              public long        partEraseSize       = -1;

                              public MemBankSpec memBankSpec         = new MemBankSpec(); // Optional; only needs to be specified for some MCUs

                              public int[]       readDataBuff        = null;
        }

        public static class MemoryEEPROM implements Serializable {
            @DataFormat.Hex08 public long address   = -1;
                              public int  totalSize =  0;
                              public int  pageSize  =  0;
                              public int  numPages  =  0;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final CortexMReg   cortexMReg   = new CortexMReg  ();
        public final MemorySRAM   memorySRAM   = new MemorySRAM  ();
        public final MemoryFlash  memoryFlash  = new MemoryFlash ();
        public final MemoryEEPROM memoryEEPROM = new MemoryEEPROM();

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////

        // ##### !!! TODO !!! #####
        //#include "config.inc/SWD.*.java.inc"

    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("this-escape")
    protected       SWDExecInst _swdExecInst = new SWDExecInst(ProgClassName, this);

    protected final USB2GPIO    _usb2gpio;
    protected       int         _swdClockFrequency;
    protected       Config      _config;

    protected       boolean     _inProgMode  = false;
    protected       boolean     _chipErased  = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _checkConfig(final Config config) throws Exception
    {
        // Check the configuration values
        if(config.memorySRAM.address   <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMRAddress, ProgClassName);
        if(config.memorySRAM.totalSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMRTotSize, ProgClassName);

        if( (config.memorySRAM.memBankSpec.numBanks > 0) ) {
            if(config.memorySRAM.memBankSpec.address == null || config.memorySRAM.memBankSpec.address.length != config.memorySRAM.memBankSpec.numBanks) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMRBankAddr, ProgClassName);
            if(config.memorySRAM.memBankSpec.size    == null || config.memorySRAM.memBankSpec.size.length    != config.memorySRAM.memBankSpec.numBanks) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMRBankSize, ProgClassName);
            int sumSize = 0;
            for(final int z : config.memorySRAM.memBankSpec.size) sumSize += z;
            if(sumSize != config.memorySRAM.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMRBankSize, ProgClassName);
        }

        if(config.memoryFlash.address   <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFAddress , ProgClassName);
        if(config.memoryFlash.totalSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFTotSize , ProgClassName);
        if(config.memoryFlash.pageSize  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSize, ProgClassName);
        if(config.memoryFlash.numPages  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFNumPages, ProgClassName);

        if(config.memoryFlash.pageSize * config.memoryFlash.numPages != config.memoryFlash.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSpec, ProgClassName);

        if( (config.memoryFlash.partEraseAddressBeg >= 0) && (config.memoryFlash.partEraseSize > 0) ) {
            if( (  config.memoryFlash.partEraseAddressBeg                                 <  0 ) ||
                ( (config.memoryFlash.partEraseAddressBeg % config.memoryFlash.pageSize) != 0 )
            ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPrtEAddr, ProgClassName);

            if( (  config.memoryFlash.partEraseSize                                      <= 0 ) ||
                ( (config.memoryFlash.partEraseSize       % config.memoryFlash.pageSize) != 0 )
            ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPrtESize, ProgClassName);
        }

        if( (config.memoryFlash.memBankSpec.numBanks > 0) ) {
            if(config.memoryFlash.memBankSpec.address == null || config.memoryFlash.memBankSpec.address.length != config.memoryFlash.memBankSpec.numBanks) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBankAddr, ProgClassName);
            if(config.memoryFlash.memBankSpec.size    == null || config.memoryFlash.memBankSpec.size.length    != config.memoryFlash.memBankSpec.numBanks) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBankSize, ProgClassName);
            int sumSize = 0;
            for(final int z : config.memoryFlash.memBankSpec.size) sumSize += z;
            if(sumSize != config.memoryFlash.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFBankSize, ProgClassName);
        }

        if(config.memoryEEPROM.address >= 0 || config.memoryEEPROM.totalSize > 0) {
            if(config.memoryEEPROM.address   <  0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEAddress , ProgClassName);
            if(config.memoryEEPROM.totalSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMETotSize , ProgClassName);
            if(config.memoryEEPROM.pageSize  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEPageSize, ProgClassName);
            if(config.memoryEEPROM.numPages  <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMENumPages, ProgClassName);

            if(config.memoryEEPROM.pageSize * config.memoryEEPROM.numPages != config.memoryEEPROM.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMEPageSpec, ProgClassName);
        }
    }

    protected ProgSWDLowLevel(final USB2GPIO usb2gpio, final Config config) throws Exception
    {
        // Store the objects
        _usb2gpio = usb2gpio;
        _config   = config.deepClone();

        // Check the configuration values
        _checkConfig(_config);
    }

    public Config setConfig(Config config) throws Exception
    {
        // Save the original config
        final Config orgConfig = _config;

        // Check and stroe the new config
        _checkConfig(config);
        _config = config.deepClone();

        // Return the original config
        return orgConfig;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final boolean SWD_CMD_WRITE_PREPAD         = true; // Set it to 'false' to use post-padding

    protected static final int     SWD_INIT_RETRY_COUNT         = 16;
    protected static final int     SWD_RDWR_ADP_REG_RETRY_COUNT =  3;

    protected static final int     SWD_INIT_WAIT_PUDBGIFC_MS    = 3000;

    protected static final int     SWD_ACK_OK                   = 0x4; // 0b100
    protected static final int     SWD_ACK_WAIT                 = 0x2; // 0b010
    protected static final int     SWD_ACK_FAULT                = 0x1; // 0b001
    protected static final int     SWD_ACK_PROTOCOL_ERROR       = 0x7; // 0b111

    protected static final long    CTRLSTAT_CDBGRSTREQ          = 0x04000000L;
    protected static final long    CTRLSTAT_CDBGRSTACK          = 0x08000000L;
    protected static final long    CTRLSTAT_CDBGPWRUPREQ        = 0x10000000L;
    protected static final long    CTRLSTAT_CDBGPWRUPACK        = 0x20000000L;
    protected static final long    CTRLSTAT_CSYSPWRUPREQ        = 0x40000000L;
    protected static final long    CTRLSTAT_CSYSPWRUPACK        = 0x80000000L;
    protected static final long    CTRLSTAT_STICKYERR           = 0x00000020L;

    protected static enum MemAP {

        //     ap   bank offset
        CSW  ( 0x0, 0x0, 0x0 ), // Control/Status Word Register
        TAR  ( 0x0, 0x0, 0x4 ), // Transfer Address    Register [31:00]
        TAR1 ( 0x0, 0x0, 0x8 ), // Transfer Address    Register [63:32]
        DRW  ( 0x0, 0x0, 0xC ), // Data Read/Write     Register

        BD0  ( 0x0, 0x1, 0x0 ), // Banked Data 0       Register
        BD1  ( 0x0, 0x1, 0x4 ), // Banked Data 1       Register
        BD2  ( 0x0, 0x1, 0x8 ), // Banked Data 2       Register
        BD3  ( 0x0, 0x1, 0xC ), // Banked Data 3       Register

        CFG  ( 0x0, 0xF, 0x4 ), // Configuration       Register
        BASE ( 0x0, 0xF, 0x8 ), // Debug Base Address  Register [31:00]
        BASE1( 0x0, 0xF, 0x0 ), // Debug Base Address  Register [63:32]
        IDR  ( 0x0, 0xF, 0xC ), // Identification      Register

        // End marker
        __END__()

        ;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final String name;
        public final int    ap;
        public final int    bank;
        public final int    offset;

        private MemAP(final int ap_, final int bank_, final int offset_)
        {
            name   = name();
            ap     = ap_;
            bank   = bank_;
            offset = offset_;
        }

        private MemAP(final int bank_, final int offset_)
        { this(0, bank_, offset_); }

        private MemAP()
        { this(-1, -1, -1); }

    } // enum MemAP

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class CoreReg {

        public static final int R0   = 0x00;
        public static final int R1   = 0x01;
        public static final int R2   = 0x02;
        public static final int R3   = 0x03;
        public static final int R4   = 0x04;
        public static final int R5   = 0x05;
        public static final int R6   = 0x06;
        public static final int R7   = 0x07; // Holds Syscall Number
        public static final int R8   = 0x08;
        public static final int R9   = 0x09;
        public static final int R10  = 0x0A;

        public static final int R11  = 0x0B;
        public static final int FP   = 0x0B; // Frame Pointer

        public static final int R12  = 0x0C;
        public static final int IP   = 0x0C; // Intra-Procedural-Call Scratch

        public static final int R13  = 0x0D;
        public static final int SP   = 0x0D; // Stack Pointer

        public static final int R14  = 0x0E;
        public static final int LR   = 0x0E; // Link Register

        public static final int R15  = 0x0F;
        public static final int PC   = 0x0F; // Program Counter

        public static final int R16  = 0x10;
        public static final int XPSR = 0x10;

        public static final int MSP  = 0x11; // Main    Stack Pointer
        public static final int PSP  = 0x12; // Process Stack Pointer
        public static final int SFR  = 0x14; // 4 in 1 SFR -> [31:24] CONTROL ; [23:16] FAULTMASK ; [15:08] BASEPRI ; [07:00] PRIMASK

        // The registers below are for ARMv8-M and later only
        public static final int MSP_NS    = 0x18; // Main    Stack Pointer
        public static final int PSP_NS    = 0x19; // Process Stack Pointer

        public static final int MSP_S     = 0x1A; // Main    Stack Pointer
        public static final int PSP_S     = 0x1B; // Process Stack Pointer

        public static final int MSPLIM_S  = 0x1C; // Main    Stack Pointer Limit
        public static final int PSPLIM_S  = 0x1D; // Process Stack Pointer Limit

        public static final int MSPLIM_NS = 0x1E; // Main    Stack Pointer Limit
        public static final int PSPLIM_NS = 0x1F; // Process Stack Pointer Limit

        public static final int SFR_S     = 0x22; // 4 in 1 SFR
        public static final int SFR_NS    = 0x23; // 4 in 1 SFR

    } // class CoreReg

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static String _strAckCode(final int ackCode)
    {
        switch(ackCode) {
            case SWD_ACK_OK             : return "OK";
            case SWD_ACK_WAIT           : return "WAIT";
            case SWD_ACK_FAULT          : return "FAULT";
            case SWD_ACK_PROTOCOL_ERROR : return "PROTOCOL_ERROR";
            default                     : return "<UNKNOWN_ERROR>";
        }
    }

    protected static String _strAckCode(final long ackCode)
    { return _strAckCode( (int) ackCode ); }

    protected static void _printf08Xn(final String prefix, final long value1)
    { SysUtil.stdDbg().printf("%s%08X\n", prefix, value1); }

    protected static void _printf08Xn(final String prefix, final long value1, final long value2)
    { SysUtil.stdDbg().printf("%s%08X %08X\n", prefix, value1, value2); }

    protected static void _printf08Xn(final String prefix, final long value1, final long value2, final long value3)
    { SysUtil.stdDbg().printf("%s%08X %08X %08X\n", prefix, value1, value2, value3); }

    protected static void _printf08Xn(final long value1)
    { _printf08Xn("", value1); }

    protected static void _printf08Xn(final long value1, final long value2)
    { _printf08Xn("", value1, value2); }

    protected static void _printf08Xn(final long value1, final long value2, final long value3)
    { _printf08Xn("", value1, value2, value3); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final int[] _cmdSwitchToSWDPv1 = new int[] {
                   0xFF ,            0xFF , 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, // Line reset (clocking at least 50 cycles with SWDIO high)
        XCom._RU08(0x9E), XCom._RU08(0xE7),                               // JTAG-to-SWD switching sequence
                   0xFF ,            0xFF , 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, // Line reset (clocking at least 50 cycles with SWDIO high)
                   0x00                                                   // Idle       (clocking at least  2 cycles with SWDIO low )
    };

    protected static final int[] _cmdSwitchToSWDPv2_prefix = new int[] {
        0xFF, 0xFF, 0xFF, 0xFF,                          // Selection alert detection reset (clocking at least 8 cycles with SWDIO high             )
        0x49, 0xCF, 0x90, 0x46, 0xA9, 0xB4, 0xA1, 0x61,  // Selection alert 0x49CF9046 0xA9B4A161 0x97F5BBC7 0x45703D98 (MSB first)
        0x97, 0xF5, 0xBB, 0xC7, 0x45, 0x70, 0x3D, 0x98,  // ---
        0x05, 0x8F,                                      // 4 cycles with SWDIO low + SWD activation code 0x58 (MSB first) + 4 cycles with SWDIO high
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,        // Line reset (clocking at least 50 cycles with SWDIO high)
        0x00                                             // Idle       (clocking at least  2 cycles with SWDIO low )
    };

    protected static final int[] _cmdSWDLineReset = new int[] {
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, // Line reset (clocking at least 50 cycles with SWDIO high)
        0x00                                      // Idle       (clocking at least  2 cycles with SWDIO low )
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected int _cmdRd_lastAckRes = -1;

    protected int _cmdRdRes_unpackAck(final int[] buff, final int offset)
    {
        _cmdRd_lastAckRes = (buff[offset + 1] >> 2) & 0x07;

        return _cmdRd_lastAckRes;
    }

    protected int _cmdRdRes_unpackAck(final int[] buff)
    { return _cmdRdRes_unpackAck(buff, 0); }

    protected long _cmdRdRes_unpackUInt32(final int[] buff, final int offset)
    {
        /*
         * Buffer-Byte#   0        1        2        3        4        5
         *                00...... ......DD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDP.
         *                  SARAAP SPTAAADD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDPT
         *
         * UInt32-Byte#   3       2       1       0
         *                76543210765432107654321076543210
         *                11
         *                  111111110000000000000000000000
         *                          1111111100000000000000
         *                                  11111111000000
         *                                          111111
         *                76543210765432107654321076543210
         */

        final long res = XCom._RU32(
            ( ( (long) (buff[offset + 1] & 0x03) ) << 30 ) |
            ( ( (long)  buff[offset + 2]         ) << 22 ) |
            ( ( (long)  buff[offset + 3]         ) << 14 ) |
            ( ( (long)  buff[offset + 4]         ) <<  6 ) |
            ( ( (long)  buff[offset + 5]         ) >>  2 )
        );

        if( XCom.parityU32(res) != ( (buff[offset + 5] >> 1) & 0x01 ) ) {
            USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_RdParErr, ProgClassName);
            return -1;
        }

        return res;
    }

    protected long _cmdRdRes_unpackUInt32(final int[] buff)
    { return _cmdRdRes_unpackUInt32(buff, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected int _cmdWr_lastAckRes = -1;

    protected int _cmdWrRes_unpackAck(final int[] buff)
    {
         _cmdWr_lastAckRes =  SWD_CMD_WRITE_PREPAD ? ( (buff[1] >> 2) & 0x07 )
                                                   : ( (buff[1] >> 4) & 0x07 );

         return _cmdWr_lastAckRes;
    }

    protected static void _cmdWr_packUInt32(final int[] buff, final int offset, final long value)
    {
        final long rev = XCom._RU32(value);
        final int  par = XCom.parityU32(rev);

        if(SWD_CMD_WRITE_PREPAD) {
            /*
             * Buffer-Byte#   0        1        2        3        4        5
             *                00...... .......D DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
             *                  SAWAAP SPTAAATD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
             *
             * UInt32-Byte#   3       2       1       0
             *                76543210765432107654321076543210
             *                1
             *                 1111111100000000000000000000000
             *                         11111111000000000000000
             *                                 111111110000000
             *                                         1111111
             *                76543210765432107654321076543210
             */
            buff[offset + 1] |= (int) ( (rev & 0x80000000L) >> 31 );
            buff[offset + 2]  = (int) ( (rev & 0x7F800000L) >> 23 );
            buff[offset + 3]  = (int) ( (rev & 0x007F8000L) >> 15 );
            buff[offset + 4]  = (int) ( (rev & 0x00007F80L) >>  7 );
            buff[offset + 5]  = (int) ( (rev & 0x0000007FL) <<  1 );
            buff[offset + 5] |=       (  par                <<  0 );
        }

        else {
            /*
             * Buffer-Byte#   0        1        2        3        4        5
             *                ........ .....DDD DDDDDDDD DDDDDDD DDDDDDDDD DDDDDP00
             *                SAWAAPSP TAAATDDD DDDDDDDD DDDDDDD DDDDDDDDD DDDDDP
             *
             * UInt32-Byte#   3       2       1       0
             *                76543210765432107654321076543210
             *                111
             *                   11111111000000000000000000000
             *                           111111110000000000000
             *                                   1111111100000
             *                                           11111
             *                76543210765432107654321076543210
             */
            buff[offset + 1] |= (int) ( (rev & 0xE0000000L) >> 29 );
            buff[offset + 2]  = (int) ( (rev & 0x1FE00000L) >> 21 );
            buff[offset + 3]  = (int) ( (rev & 0x001FE000L) >> 13 );
            buff[offset + 4]  = (int) ( (rev & 0x00001FE0L) >>  5 );
            buff[offset + 5]  = (int) ( (rev & 0x0000001FL) <<  3 );
            buff[offset + 5] |=       (  par                <<  2 );
        }
    }

    protected static void _cmdWr_packUInt32(final int[] buff, final long value)
    { _cmdWr_packUInt32(buff, 0, value); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final int[] _cmdRdDP_DPIDR = new int[] { // 0x00
        /*
         *           HOST                                         TARGET
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   D31...D00 Parity Trn
         *           1     0     1   0  0  1      0    1      X   1    0    0      ......... .      X
         * Bus       1     0     1   0  0  1      0    1      1   1    1    1      1.......1 1      1
         *
         * Byte#     0        1        2        3        4        5
         * Bit#      76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10100101 11111111 11111111 11111111 11111111 111111
         *           SDRAAPSP TAAADDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDPT
         * Pre-Pad   00101001 01111111 11111111 11111111 11111111 11111111
         *             SDRAAP SPTAAADD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDPT
         *
         */
        0x29, 0x7F, 0xFF, 0xFF, 0xFF, 0xFF
        /*
         * Example response:
         * Byte#     0        1        2        3        4        5
         * Bit#      76543210 76543210 76543210 76543210 76543210 76543210
         * Hex-Val   29       73       B8       A0       17       51
         * Bin-Val   00101001 01110011 10111000 10100000 00010111 01010001 (Pre-Padded)
         *             SDRAAP SPTAAADD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDPT
         */
    };

    protected static final int[] _cmdRdDP_REG_0X04 = new int[] { // 0x04
        /*
         *           HOST                                         TARGET
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   D31...D00 Parity Trn
         *           1     0     1   1  0  0      0    1      X   1    0    0      ......... .      X
         * Bus       1     0     1   1  0  0      0    1      1   1    1    1      1.......1 1      1
         *
         * Byte#     0        1        2        3        4        5
         * Bit#      76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10110001 11111111 11111111 11111111 11111111 111111
         *           SDRAAPSP TAAADDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDPT
         * Pre-Pad   00101100 01111111 11111111 11111111 11111111 11111111
         *             SDRAAP SPTAAADD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDPT
         */
        0x2C, 0x7F, 0xFF, 0xFF, 0xFF, 0xFF
      //0xB1, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
    };

    protected static final int[] _cmdRdDP_RESEND = new int[] { // 0x08
        /*
         *           HOST                                         TARGET
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   D31...D00 Parity Trn
         *           1     0     1   0  1  0      0    1      X   1    0    0      ......... .      X
         * Bus       1     0     1   0  1  0      0    1      1   1    1    1      1.......1 1      1
         *
         * Byte#     0        1        2        3        4        5
         * Bit#      76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10101001 11111111 11111111 11111111 11111111 111111
         *           SDRAAPSP TAAADDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDPT
         * Pre-Pad   00101010 01111111 11111111 11111111 11111111 11111111
         *             SDRAAP SPTAAADD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDPT
         */
        0x2A, 0x7F, 0xFF, 0xFF, 0xFF, 0xFF
    };

    protected static final int[] _cmdRdDP_RDBUFF = new int[] { // 0x0C
        /*
         *           HOST                                         TARGET
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   D31...D00 Parity Trn
         *           1     0     1   1  1  1      0    1      X   1    0    0      ......... .      X
         * Bus       1     0     1   1  1  1      0    1      1   1    1    1      1.......1 1      1
         *
         * Byte#     0        1        2        3        4        5
         * Bit#      76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10111101 11111111 11111111 11111111 11111111 111111
         *           SDRAAPSP TAAADDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDPT
         * Pre-Pad   00101111 01111111 11111111 11111111 11111111 11111111
         *             SDRAAP SPTAAADD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDPT
         */
        0x2F, 0x7F, 0xFF, 0xFF, 0xFF, 0xFF
    };

    protected static final int[] _cmdRdMemAP = new int[] {
        /*
         *           HOST                                         TARGET
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   D31...D00 Parity Trn
         *           1     1     1   A  A  0      0    1      X   1    0    0      ......... .      X
         * Bus       1     1     1   A  A  0      0    1      1   1    1    1      1.......1 1      1
         *
         * Byte#     0        1        2        3        4        5
         * Bit#      76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   111AA001 11111111 11111111 11111111 11111111 111111
         *           SARAAPSP TAAADDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDPT
         * Pre-Pad   00111AA0 01111111 11111111 11111111 11111111 11111111
         *             SARAAP SPTAAADD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDPT
         */
        0x38, 0x7F, 0xFF, 0xFF, 0xFF, 0xFF
    };

    protected long _swdRdADPReg(final int[] cmdBuff, final int offset)
    {
        // Copy the command buffer
        final int[] cbuff = XCom.arrayConcatCopy(cmdBuff);

        // Pack the offset and parity as needed
        if(offset >= 0) {
            final int a3 = (offset & 0x08) >> 3;
            final int a2 = (offset & 0x04) >> 2;
            final int p  = a3 ^ a2;
            cbuff[0] |= a3 << 1;
            cbuff[0] |= a2 << 2;
            cbuff[0] ^= p  << 0;
        }

        // Retry the command several times
        int ack = 0;

        for(int i = 0; i < SWD_RDWR_ADP_REG_RETRY_COUNT; ++i) {

            // Send the command and read the response
            final int[] tbuff = XCom.arrayConcatCopy(cbuff);

            if( !_usb2gpio.spiTransfer(tbuff) ) return -1;

            // Check and read the response
            ack = _cmdRdRes_unpackAck(tbuff);

            if(ack == SWD_ACK_OK  ) return _cmdRdRes_unpackUInt32(tbuff);
            if(ack != SWD_ACK_WAIT) break;

        } // for

        USB2GPIO.notifyError( Texts.ProgXXX_FailSWD_RdAckNOK, ProgClassName, ack, _strAckCode(ack) );

        // Not done
        return -1;
    }

    protected long[] _swdRdADPReg(final int[] cmdBuff, final int[] offset)
    {
        // Check the length
        if(cmdBuff.length % offset.length != 0) return null;

        // Copy the command buffer
        final int[] cbuff = XCom.arrayConcatCopy(cmdBuff);

        // Pack the offset and parity as needed
        final int MF = cmdBuff.length / offset.length;

        for(int i = 0; i < offset.length; ++i) {
            if(offset[i] >= 0) {
                final int a3 = (offset[i] & 0x08) >> 3;
                final int a2 = (offset[i] & 0x04) >> 2;
                final int p  = a3 ^ a2;
                cbuff[MF * i] |= a3 << 1;
                cbuff[MF * i] |= a2 << 2;
                cbuff[MF * i] ^= p  << 0;
            }
        }

        // Send the command
        if( !_usb2gpio.spiTransfer(cbuff) ) return null;

        // Check and read the response
        final long[] res = new long[offset.length];

        for(int i = 0; i < offset.length; ++i) {
            final int ack = _cmdRdRes_unpackAck(cbuff, MF * i);
            if(ack != SWD_ACK_OK) {
                USB2GPIO.notifyError( Texts.ProgXXX_FailSWD_RdAckNOK, ProgClassName, ack, _strAckCode(ack) );
                return null;
            }
            res[i] = _cmdRdRes_unpackUInt32(cbuff, MF * i);
        }

        // Done
        return res;
    }

    protected long _swdRdDP_IDCODE() throws USB2GPIO.TansmitError
    {
        /*
         * Bit#   31...28    27...20   19...17   16    15...12   11...1          0
         *        REVISION   PARTNO    0...0     MIN   VERSION   DESIGNER        RAO
         *        0x?        0x??      0b000     0b?   0x?       0b???????????   0b?
         */
        _swdWrDP_SELECT(0, -1, 0, false);

        final long res = _swdRdADPReg(_cmdRdDP_DPIDR, -1);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdIDCODE, ProgClassName);
        return res;
    }

    protected long _swdRdDP_CTRLSTAT() throws USB2GPIO.TansmitError
    {
        _swdWrDP_SELECT(0, -1, 0, false);

        final long res = _swdRdADPReg(_cmdRdDP_REG_0X04, -1);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdCTRLST, ProgClassName);
        return res;
    }

    protected long _swdRdDP_DLCR() throws USB2GPIO.TansmitError
    {
        _swdWrDP_SELECT(0, -1, 1, false);

        final long res = _swdRdADPReg(_cmdRdDP_REG_0X04, -1);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdDLCR, ProgClassName);
        return res;
    }

    protected long _swdRdDP_TARGETID() throws USB2GPIO.TansmitError
    {
        /*
         * Bit#   31...28     27...12   11...1          0
         *        TREVISION   TPARTNO   TDESIGNER       1
         *        0x?         0x????    0b???????????   1
         */

        _swdWrDP_SELECT(0, -1, 2, false);

        final long res = _swdRdADPReg(_cmdRdDP_REG_0X04, -1);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdTRGTID, ProgClassName);
        return res;
    }

    protected long _swdRdDP_DLPIDR() throws USB2GPIO.TansmitError
    {
        /*
         * Bit#   31...28     27...4       3...0
         *        TINSTANCE   <RESERVED>   PROTVSN
         *        0x?         0x000000     0x1
         */

        _swdWrDP_SELECT(0, -1, 3, false);

        final long res = _swdRdADPReg(_cmdRdDP_REG_0X04, -1);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdDLPIDR, ProgClassName);
        return res;
    }

    protected long _swdRdDP_EVENTSTAT() throws USB2GPIO.TansmitError
    {
        /*
         * Bit#   31...1       0
         *        <RESERVED>   EA
         *        0b0...0      0b0 ->  there is an event that requires attention
         *                     0b1 ->  there is no event that requires attention
         */

        _swdWrDP_SELECT(0, -1, 4, false);

        final long res = _swdRdADPReg(_cmdRdDP_REG_0X04, -1);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdEVENST, ProgClassName);
        return res;
    }

    protected long _swdRdDP_RESEND() throws USB2GPIO.TansmitError
    {
        final long res = _swdRdADPReg(_cmdRdDP_RESEND, -1);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdRESEND, ProgClassName);
        return res;
    }

    protected long _swdRdDP_RDBUFF() throws USB2GPIO.TansmitError
    {
        final long res = _swdRdADPReg(_cmdRdDP_RDBUFF, -1);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdRDBUFF, ProgClassName);
        return res;
    }

    protected long _swdRdMemAP_impl(final MemAP memAP) throws USB2GPIO.TansmitError
    {
        _swdWrDP_SELECT(memAP.ap, memAP.bank, -1, true);

        final long res = _swdRdADPReg(_cmdRdMemAP, memAP.offset);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_RdRegAP, ProgClassName, memAP.name, _cmdRd_lastAckRes, _strAckCode(_cmdRd_lastAckRes) );
        return res;
    }

    protected long _swdRdMemAP(final MemAP memAP) throws USB2GPIO.TansmitError
    {
               _swdRdMemAP_impl(memAP);
        return _swdRdDP_RDBUFF (     );
    }

    protected long[] _swdRdMemAP(final MemAP memAP, final int repCnt) throws USB2GPIO.TansmitError
    {
        // Generate the command and offset buffers
        final int   grpLen = _cmdRdMemAP.length + _cmdRdDP_RDBUFF.length;

        final int[] cmdBuf = new int[repCnt * grpLen];
        final int[] ofsBuf = new int[repCnt * 2     ];

        for(int i = 0; i < repCnt; ++i) {
            XCom.arrayCopy(cmdBuf, i * grpLen                     , _cmdRdMemAP    ); ofsBuf[i * 2 + 0] = memAP.offset;
            XCom.arrayCopy(cmdBuf, i * grpLen + _cmdRdMemAP.length, _cmdRdDP_RDBUFF); ofsBuf[i * 2 + 1] = -1;
        }

        // Send the command and check the response
        _swdWrDP_SELECT(memAP.ap, memAP.bank, -1, true);

        final long[] res = _swdRdADPReg(cmdBuf, ofsBuf);

        if(res == null) USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_RdRegAP, ProgClassName, memAP.name, _cmdRd_lastAckRes, _strAckCode(_cmdRd_lastAckRes) );

        // Extract and return the values
        final long[] val = new long[repCnt];

        for(int i = 0; i < repCnt; ++i) val[i] = res[i * 2 + 1];

        return val;
    }

    protected long[] _swdRdMemAP_DRW(final int repCnt) throws USB2GPIO.TansmitError
    { return _swdRdMemAP(MemAP.DRW, repCnt); }

    private long _swdRdRawMemAP(final int offset, final String rawMemAPName) throws USB2GPIO.TansmitError
    {
        final long res = _swdRdADPReg(_cmdRdMemAP, offset);
        if(res < 0) USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_RdRegAP, ProgClassName, rawMemAPName, _cmdRd_lastAckRes, _strAckCode(_cmdRd_lastAckRes) );

        return _swdRdDP_RDBUFF();
    }

    protected synchronized long _swdRdRawMemAP(final int ap, final int bank_offset, final String rawMemAPName_) throws USB2GPIO.TansmitError
    {
        final String rawMemAPName = (rawMemAPName_ != null) ? rawMemAPName_ : String.format("RD:MEM-AP[%03d]:REG[%02X]", ap, bank_offset);

        _swdWrDP_SELECT( ap, (bank_offset >> 4) & 0x0F, 0x0, true );

        return _swdRdRawMemAP(bank_offset & 0x0F, rawMemAPName);
    }

    protected synchronized long _swdRdRawMemAP(final int ap, final int bank_offset) throws USB2GPIO.TansmitError
    { return _swdRdRawMemAP(ap, bank_offset, null); }

    protected synchronized long _swdRdRawMemAP3(final long address28_4, final String rawMemAPName_) throws USB2GPIO.TansmitError
    {
        // !!! WARNING : DPv3 only !!!

        final String rawMemAPName = (rawMemAPName_ != null) ? rawMemAPName_ : String.format("RD:MEM-AP[%08X]", address28_4);

        if( !_swdWrADPReg( _cmdWrDP_SELECT,       -1                , address28_4 & 0xFFFFFFF0L )     ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrSELECT, ProgClassName);
        if(  _swdRdADPReg( _cmdRdMemAP    , (int) address28_4 & 0x0F                            ) < 0 ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdRegAP , ProgClassName, rawMemAPName, _cmdRd_lastAckRes, _strAckCode(_cmdRd_lastAckRes) );

        return _swdRdDP_RDBUFF();
    }

    protected synchronized long _swdRdRawMemAP3(final long address28_4) throws USB2GPIO.TansmitError
    { return _swdRdRawMemAP3(address28_4, null); }

    @package_private synchronized
    long _swdRdBus(final long address) throws USB2GPIO.TansmitError
    {
               _swdWrMemAP(MemAP.TAR, address);
        return _swdRdMemAP(MemAP.DRW         );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final int[] _cmdWrDP_ABORT_clearError = new int[] { // 0x00
        /*
         *           HOST                                         TARGET               HOST
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   Trn D31...D00 Parity
         *           1     0     0   0  0  0      0    1      X   1    0    0      X   ......... .
         * Bus       1     0     0   0  0  0      0    1      1   1    1    1      1   0x00...1E 0
         *
         * Byte      0        1        2        3        4        5
         * Bit       76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10000001 11111000 00000000 00000000 00000000 111100
         *           SDWAAPSP TAAATDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Rev-Dat   10000001 11111011 11000000 00000000 00000000 000000
         * Pre-Pad   00100000 01111110 11110000 00000000 00000000 00000000
         *             SDWAAP SPTAAATD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         */
         SWD_CMD_WRITE_PREPAD ? 0x20 : 0x81,
         SWD_CMD_WRITE_PREPAD ? 0x7E : 0xF8,
         SWD_CMD_WRITE_PREPAD ? 0xF0 : 0xC0,
         0x00, 0x00, 0x00
    };

    protected static final int[] _cmdWrDP_ABORT_abort = new int[] { // 0x00
        /*
         *           HOST                                         TARGET               HOST
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   Trn D31...D00 Parity
         *           1     0     0   0  0  0      0    1      X   1    0    0      X   ......... .
         * Bus       1     0     0   0  0  0      0    1      1   1    1    1      1   0x00...1F 1
         *
         * Byte      0        1        2        3        4        5
         * Bit       76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10000001 11111000 00000000 00000000 00000000 111111
         *           SDWAAPSP TAAATDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Rev-Dat   10000001 11111111 11000000 00000000 00000000 000001
         * Pre-Pad   00100000 01111111 11110000 00000000 00000000 00000001
         *             SDWAAP SPTAAATD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         */
         SWD_CMD_WRITE_PREPAD ? 0x20 : 0x81,
         SWD_CMD_WRITE_PREPAD ? 0x7F : 0xFF,
         SWD_CMD_WRITE_PREPAD ? 0xF0 : 0xC0,
         SWD_CMD_WRITE_PREPAD ? 0x00 : 0x00,
         SWD_CMD_WRITE_PREPAD ? 0x00 : 0x00,
         SWD_CMD_WRITE_PREPAD ? 0x01 : 0x04
    };

    protected static final int[] _cmdWrDP_REG_0x00 = new int[] { // 0x00
        /*
         *           HOST                                         TARGET               HOST
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   Trn D31...D00 Parity
         *           1     0     0   0  0  0      0    1      X   1    0    0      X   ......... .
         * Bus       1     0     0   0  0  0      0    1      1   1    1    1      1   ......... .
         *
         * Byte      0        1        2        3        4        5
         * Bit       76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10000001 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         *           SDWAAPSP TAAATDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Rev-Dat   10000001 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Pre-Pad   00100000 0111111D DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         *             SDWAAP SPTAAATD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         */
         SWD_CMD_WRITE_PREPAD ? 0x20 : 0x81,
         SWD_CMD_WRITE_PREPAD ? 0x7E : 0xF8,
         0x00, 0x00, 0x00, 0x00
    };

    protected static final int[] _cmdWrDP_REG_0x04 = new int[] { // 0x04
        /*
         *           HOST                                         TARGET               HOST
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   Trn D31...D00 Parity
         *           1     0     0   1  0  1      0    1      X   1    0    0      X   ......... .
         * Bus       1     0     0   1  0  1      0    1      1   1    1    1      1   ......... .
         *
         * Byte      0        1        2        3        4        5
         * Bit       76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10010101 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         *           SDWAAPSP TAAATDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Rev-Dat   10010101 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Pre-Pad   00100101 0111111D DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         *             SDWAAP SPTAAATD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         */
         SWD_CMD_WRITE_PREPAD ? 0x25 : 0x95,
         SWD_CMD_WRITE_PREPAD ? 0x7E : 0xF8,
         0x00, 0x00, 0x00, 0x00
    };

    protected static final int[] _cmdWrDP_SELECT = new int[] { // 0x08
        /*
         *           HOST                                         TARGET               HOST
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   Trn D31...D00 Parity
         *           1     0     0   0  1  1      0    1      X   1    0    0      X   ......... .
         * Bus       1     0     0   0  1  1      0    1      1   1    1    1      1   ......... .
         *
         * Byte      0        1        2        3        4        5
         * Bit       76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10001101 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         *           SDWAAPSP TAAATDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Rev-Dat   10001101 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Pre-Pad   00100011 0111111D DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         *             SDWAAP SPTAAATD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         */
         SWD_CMD_WRITE_PREPAD ? 0x23 : 0x8D,
         SWD_CMD_WRITE_PREPAD ? 0x7E : 0xF8,
         0x00, 0x00, 0x00, 0x00
    };

    protected static final int[] _cmdWrDP_TARGETSEL = new int[] { // 0x0C
        /*
         *           HOST                                         TARGET               HOST
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   Trn D31...D00 Parity
         *           1     0     0   1  1  0      0    1      X   1    0    0      X   ......... .
         * Bus       1     0     0   1  1  0      0    1      1   1    1    1      1   ......... .
         *
         * Byte      0        1        2        3        4        5
         * Bit       76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   10011001 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         *           SDWAAPSP TAAATDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Rev-Dat   10011001 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Pre-Pad   00100110 0111111D DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         *             SDWAAP SPTAAATD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         */
        SWD_CMD_WRITE_PREPAD ? 0x26 : 0x99,
        SWD_CMD_WRITE_PREPAD ? 0x7E : 0xF8,
        0x00, 0x00, 0x00, 0x00
    };

    protected static final int[] _cmdWrMemAP = new int[] {
        /*
         *           HOST                                         TARGET               HOST
         * 46 bits   Start AP/D̅P̅ R/W̅ A2 A3 Parity Stop Park   Trn Ack2 Ack1 Ack0   Trn D31...D00 Parity
         *           1     1     0   A  A  1      0    1      X   1    0    0      X   ......... .
         * Bus       1     1     0   A  A  1      0    1      1   1    1    1      1   ......... .
         *
         * Byte      0        1        2        3        4        5
         * Bit       76543210 76543210 76543210 76543210 76543210 76543210
         * Bin-Val   110AA101 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         *           SAWAAPSP TAAATDDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Rev-Dat   110AA101 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP
         * Pre-Pad   00110AA1 0111111D DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         * Pst-Pad   110AA101 11111DDD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDP00
         *             SAWAAP SPTAAATD DDDDDDDD DDDDDDDD DDDDDDDD DDDDDDDP
         */
         SWD_CMD_WRITE_PREPAD ? 0x31 : 0xC5,
         SWD_CMD_WRITE_PREPAD ? 0x7E : 0xF8,
         0x00, 0x00, 0x00, 0x00
    };

    protected boolean _swdWrADPReg(final int[] cmdBuff, final int offset, final long value)
    {
        // Copy the command buffer
        final int[] cbuff = XCom.arrayConcatCopy(cmdBuff);

        // Pack the offset and parity as needed
        if(offset >= 0) {
            final int a3 = (offset & 0x08) >> 3;
            final int a2 = (offset & 0x04) >> 2;
            final int p  = a3 ^ a2;
            if(SWD_CMD_WRITE_PREPAD) {
                cbuff[0] |= a3 << 1;
                cbuff[0] |= a2 << 2;
                cbuff[0] ^= p  << 0;
            }
            else { // Post-pad
                cbuff[0] |= a3 << 3;
                cbuff[0] |= a2 << 4;
                cbuff[0] ^= p  << 2;
            }
        }

        // Pack the value as needed
        if(value >= 0) _cmdWr_packUInt32(cbuff, value);

        /*
        for(final int i : cbuff) SysUtil.stdDbg().printf("%02X ", i);
        SysUtil.stdDbg().println();
        //*/

        // Retry the command several times
        int ack = 0;

        for(int i = 0; i < SWD_RDWR_ADP_REG_RETRY_COUNT; ++i) {

            // Send the command and read the response
            final int[] tbuff = XCom.arrayConcatCopy(cbuff);

            if( !_usb2gpio.spiTransfer(tbuff) ) return false;

            // Check the response
            ack = _cmdWrRes_unpackAck(tbuff);

            if(ack == SWD_ACK_OK  ) return true;
            if(ack != SWD_ACK_WAIT) break;

        } // for

        USB2GPIO.notifyError( Texts.ProgXXX_FailSWD_WrAckNOK, ProgClassName, ack, _strAckCode(ack) );

        // Not done
        return false;
    }

    protected boolean _swdWrADPReg(final int[] cmdBuff, final int[] offset, final long[] value)
    {
        // Check the lengths
        if(offset.length != value.length || cmdBuff.length % offset.length != 0) return false;

        // Copy the command buffer
        final int[] cbuff = XCom.arrayConcatCopy(cmdBuff);

        // Pack the offset and parity as needed
        final int MF = cmdBuff.length / offset.length;

        for(int i = 0; i < offset.length; ++i) {
            if(offset[i] >= 0) {
                final int a3 = (offset[i] & 0x08) >> 3;
                final int a2 = (offset[i] & 0x04) >> 2;
                final int p  = a3 ^ a2;
                if(SWD_CMD_WRITE_PREPAD) {
                    cbuff[MF * i] |= a3 << 1;
                    cbuff[MF * i] |= a2 << 2;
                    cbuff[MF * i] ^= p  << 0;
                }
                else { // Post-pad
                    cbuff[MF * i] |= a3 << 3;
                    cbuff[MF * i] |= a2 << 4;
                    cbuff[MF * i] ^= p  << 2;
                }
            }
        }

        // Pack the value
        for(int i = 0; i < value.length; ++i) {
            if(value[i] >= 0) _cmdWr_packUInt32(cbuff, MF * i, value[i]);
        }

        // Send the command
        if( !_usb2gpio.spiTransfer(cbuff) ) return false;

        // Check and read the response
        for(int i = 0; i < offset.length; ++i) {
            final int ack = _cmdRdRes_unpackAck(cbuff, MF * i);
            if(ack != SWD_ACK_OK) {
                USB2GPIO.notifyError( Texts.ProgXXX_FailSWD_WrAckNOK, ProgClassName, ack, _strAckCode(ack) );
                return false;
            }
        }

        // Done
        return true;
    }

    protected void _swdWrDP_ABORT(final boolean withDAPAbort) throws USB2GPIO.TansmitError
    {
        // Clear the ORUNERRCLR, WDERRCLR, STKERRCLR, and STKCMPCLR bits
        if( !_swdWrADPReg(withDAPAbort ? _cmdWrDP_ABORT_abort : _cmdWrDP_ABORT_clearError, -1, -1) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrABORT, ProgClassName);
    }

    protected void _swdWrDP_CTRLSTAT(final long value) throws USB2GPIO.TansmitError
    {
        _swdWrDP_SELECT(0, -1, 0, false);

        if( !_swdWrADPReg(_cmdWrDP_REG_0x04, -1, value) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrCTRLST, ProgClassName);
    }

    protected long _apsel     = -1;
    protected long _apbanksel = -1;
    protected long _dpbanksel = -1;

    protected void _swdWrDP_SELECT(final long apsel_, final long apbanksel_, final long dpbanksel_, final boolean forMemAP) throws USB2GPIO.TansmitError
    {
        final long apsel     = (apsel_     < 0) ? _apsel     : (apsel_     & 0xFF);
        final long apbanksel = (apbanksel_ < 0) ? _apbanksel : (apbanksel_ & 0x0F);
        final long dpbanksel = (dpbanksel_ < 0) ? _dpbanksel : (dpbanksel_ & 0x0F);

        // ##### !!! TODO : Why it must always be rewritten even if the values are the same? !!! #####
        /*
        if(apsel == _apsel && apbanksel == _apbanksel && dpbanksel == _dpbanksel) return;
        //*/

        _apsel     = (apsel     < 0) ? 0 : apsel    ;
        _apbanksel = (apbanksel < 0) ? 0 : apbanksel;
        _dpbanksel = (dpbanksel < 0) ? 0 : dpbanksel;

        /*
         * DPv0   Bit#   31...24   23...8       7...4       3...0
         *               APSEL     <RESERVED>   APBANKSEL   <RESERVED>
         *               0x00      0x0000       0x?         0b0000
         *
         * CTRL/STAT                            0x4
         * SELECT                               0x8
         * RDBUFF                               0xC
         *
         *----------------------------------------------------------------------------------------------------
         *
         * DPv1   Bit#   31...24   23...8       7...4       3...1        0
         *               APSEL     <RESERVED>   APBANKSEL   <RESERVED>   CTRLSEL
         *               0x00      0x0000       0x?         0b000        0b?
         *
         * DPIDR                                0x0                      -
         * CTRL/STAT                            0x4                      0b0
         * DLCR                                 0x4                      0b1
         * SELECT                               0x8                      -
         * RDBUFF                               0xC                      -
         *
         *----------------------------------------------------------------------------------------------------
         *
         * DPv2   Bit#   31...24   23...8       7...4       3...0
         *               APSEL     <RESERVED>   APBANKSEL   DPBANKSEL (0 is still CTRLSEL)
         *               0x00      0x0000       0x?         0x?
         *
         * DPIDR                                0x0         -
         * CTRL/STAT                            0x4         0x0
         * DLCR                                 0x4         0x1
         * TARGETID                             0x4         0x2
         * DLPIDR                               0x4         0x3
         * EVENTSTAT                            0x4         0x4
         * SELECT                               0x8         -
         * RDBUFF                               0xC         -
         *
         *----------------------------------------------------------------------------------------------------
         *
         * DPv3   Bit#   31...24   23...8       7...4       3...0
         *               -----------------------ADDRESS     DPBANKSEL (0 is still CTRLSEL)
         *                                      0x???????   0x?
         *
         * DPIDR                                0x.....00   0x0
         * DPIDR1                               0x.....00   0x1
         * BASEPTR0                             0x.....00   0x2
         * BASEPTR1                             0x.....00   0x3
         * CTRL/STAT                            0x.....04   0x0
         * DLCR                                 0x.....04   0x1
         * TARGETID                             0x.....04   0x2
         * DLPIDR                               0x.....04   0x3
         * EVENTSTAT                            0x.....04   0x4
         * SELECT1                              0x.....04   0x5
         * SELECT                               0x.....08   -
         * RDBUFF                               0x.....0C   -
         */

        if(forMemAP && _memAPOffsets != null) {
            // WARNING : # This class only handles 'DPIDR1.ASIZE' <= 32 bits!
            //           # The '_apsel' value is ignored here!
            final long memAPOffset = _memAPOffsets[_idxSelMultidropID] | ( ( _dapVer[_idxSelMultidropID] < 3 ) ? 0x0000 : 0x0D00 );
            if( !_swdWrADPReg( _cmdWrDP_SELECT, -1, (memAPOffset ) | (_apbanksel << 4) | _dpbanksel ) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrSELECT, ProgClassName);
        }
        else {
            if( !_swdWrADPReg( _cmdWrDP_SELECT, -1, (_apsel << 24) | (_apbanksel << 4) | _dpbanksel ) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrSELECT, ProgClassName);
        }
    }

    protected void _swdWrDP_TARGETSEL(final long value) throws USB2GPIO.TansmitError
    {
        /*
         * Bit#   31...28       27...12         11...1          0
         *        TINSTANCE     TPARTNO         TDESIGNER       1
         *        from DLPIDR   from TARGETID   from TARGETID
         */

        final int[] cmdWrDP_TARGETSEL = XCom.arrayConcatCopy(_cmdWrDP_TARGETSEL);

        _cmdWr_packUInt32(cmdWrDP_TARGETSEL, value);

        final int[] cbuff = XCom.arrayConcatCopy(cmdWrDP_TARGETSEL, _cmdRdDP_DPIDR);

        if( !_usb2gpio.spiTransfer(cbuff) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrTRGSEL, ProgClassName);
    }

    protected void _swdWrMemAP(final MemAP memAP, final long value) throws USB2GPIO.TansmitError
    {
        _swdWrDP_SELECT(memAP.ap, memAP.bank, -1, true);

        if( !_swdWrADPReg(_cmdWrMemAP, memAP.offset, value) ) USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_WrRegAP, ProgClassName, memAP.name, _cmdWr_lastAckRes, _strAckCode(_cmdWr_lastAckRes) );
    }

    protected void _swdWrMemAP(final MemAP memAP, final long[] value) throws USB2GPIO.TansmitError
    {
        // Generate the command and offset buffers
        final int   grpLen = _cmdWrMemAP.length;

        final int[] cmdBuf = new int[value.length * grpLen];
        final int[] ofsBuf = new int[value.length         ];

        for(int i = 0; i < value.length; ++i) {
            XCom.arrayCopy(cmdBuf, i * grpLen, _cmdWrMemAP);
            ofsBuf[i] = memAP.offset;
        }

        // Send the command and check the response
        _swdWrDP_SELECT(memAP.ap, memAP.bank, -1, true);

        if( !_swdWrADPReg(cmdBuf, ofsBuf, value) ) USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_WrRegAP, ProgClassName, memAP.name, _cmdWr_lastAckRes, _strAckCode(_cmdWr_lastAckRes) );
    }

    protected void _swdWrMemAP_DRW(final long[] value) throws USB2GPIO.TansmitError
    { _swdWrMemAP(MemAP.DRW, value); }

    private void _swdWrRawMemAP(final int offset, final long value, final String rawMemAPName) throws USB2GPIO.TansmitError
    { if( !_swdWrADPReg(_cmdWrMemAP, offset, value) ) USB2GPIO.TansmitError.throwTansmitError( Texts.ProgXXX_FailSWD_WrRegAP, ProgClassName, rawMemAPName, _cmdWr_lastAckRes, _strAckCode(_cmdWr_lastAckRes) ); }

    protected synchronized void _swdWrRawMemAP(final int ap, final int bank_offset, final long value, final String rawMemAPName_) throws USB2GPIO.TansmitError
    {
        final String rawMemAPName = (rawMemAPName_ != null) ? rawMemAPName_ : String.format("WR:MEM-AP[%03d]:REG[%02X]", ap, bank_offset);

        _swdWrDP_SELECT( ap, (bank_offset >> 4) & 0x0F, 0x0, true );

        _swdWrRawMemAP(bank_offset & 0x0F, value, rawMemAPName);
    }

    protected synchronized void _swdWrRawMemAP(final int ap, final int bank_offset, final long value) throws USB2GPIO.TansmitError
    { _swdWrRawMemAP(ap, bank_offset, value, null); }

    protected synchronized void _swdWrRawMemAP3(final long address28_4, final long value, final String rawMemAPName_) throws USB2GPIO.TansmitError
    {
        // !!! WARNING : DPv3 only !!!

        final String rawMemAPName = (rawMemAPName_ != null) ? rawMemAPName_ : String.format("WR:MEM-AP[%08X]", address28_4);

        if( !_swdWrADPReg( _cmdWrDP_SELECT,       -1                , address28_4 & 0xFFFFFFF0L ) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrSELECT, ProgClassName);
        if( !_swdWrADPReg( _cmdWrMemAP    , (int) address28_4 & 0x0F, value                     ) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrRegAP , ProgClassName, rawMemAPName, _cmdWr_lastAckRes, _strAckCode(_cmdWr_lastAckRes) );
    }

    protected synchronized void _swdWrRawMemAP3(final long address28_4, final long value) throws USB2GPIO.TansmitError
    { _swdWrRawMemAP3(address28_4, value, null); }

    @package_private synchronized
    void _swdWrBus(final long address, final long value) throws USB2GPIO.TansmitError
    {
        _swdWrMemAP(MemAP.TAR, address);
        _swdWrMemAP(MemAP.DRW, value  );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected long[] _multidropIDs      = null;
    protected int    _idxSelMultidropID = -1;

    protected long[] _memAPOffsets      = null;

    protected long[] _idcode            = null;
    protected int [] _dapVer            = null;

    protected int[] _get_cmdSwitchToSWDP()
    {
        // If no multidrop ID is specified, use DAP v1
        if(_multidropIDs == null || _idxSelMultidropID < 0) return XCom.arrayConcatCopy(_cmdSwitchToSWDPv1, _cmdRdDP_DPIDR);

        // Otherwise, use DAP v2
        if( _multidropIDs[_idxSelMultidropID] == -1 ) {
            // Do not send 'TARGETSEL' if the ID == -1
            return XCom.arrayConcatCopy(_cmdSwitchToSWDPv2_prefix, _cmdRdDP_DPIDR);
        }
        else {
            // Send 'TARGETSEL'
            final int[] cmdWrDP_TARGETSEL = XCom.arrayConcatCopy(_cmdWrDP_TARGETSEL);
            _cmdWr_packUInt32( cmdWrDP_TARGETSEL, _multidropIDs[_idxSelMultidropID] );
            return XCom.arrayConcatCopy(_cmdSwitchToSWDPv2_prefix, cmdWrDP_TARGETSEL, _cmdRdDP_DPIDR);
        }
    }

    protected void _swd_init() throws USB2GPIO.TansmitError
    {
        // Initialize SWD
        for(int i = 0; i < SWD_INIT_RETRY_COUNT; ++i) {

            // Switch to SW-DP
            if( !_usb2gpio.spiTransfer( _get_cmdSwitchToSWDP() ) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_Init, ProgClassName);

            // Clear the sticky error bit, just in case
            if( !_swdWrADPReg(_cmdWrDP_ABORT_clearError, -1, -1) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_Init, ProgClassName);

            // Done if the IDCODE can be read and it is not zero
            if( _swdRdDP_IDCODE() > 0 ) {
                // The SPI hardware may transmit artifacts upon initialization and/or uninitialization that will cause SWD errors,
                // so write ABORT and clear error(s) here
                _swdWrDP_ABORT(true);
                // Clear SELECT and CTRL/STATUS
                _swdWrDP_SELECT(0x00, 0x00, 0x00, false);
                _swdWrDP_CTRLSTAT(0x00);
                return;
            }

            // Wait for a while
            SysUtil.sleepMS(100);

        } // for

        // Not done
        USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_Init, ProgClassName);
    }

    protected void _swd_reinit() throws USB2GPIO.TansmitError
    {
        // Reinitialize SWD
        if( !_usb2gpio.spiTransfer( _get_cmdSwitchToSWDP() ) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_Init, ProgClassName);

        // Clear the sticky error bit, just in case
        if( !_swdWrADPReg(_cmdWrDP_ABORT_clearError, -1, -1) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_Init, ProgClassName);

        // Error if the IDCODE cannot be read or it is less than or equal to zero
        if( _swdRdDP_IDCODE() <= 0 ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_Init, ProgClassName);

        // Write ABORT and clear error(s)
        _swdWrDP_ABORT(true);

        // Clear SELECT
        _swdWrDP_SELECT(0x00, 0x00, 0x00, false);
    }

    protected void _swd_puDbgIfc() throws USB2GPIO.TansmitError
    {
        // Set the CSYSPWRUPREQ and CDBGPWRUPREQ bits in CTRL/STATUS to power up the debug interface
        _swdWrDP_CTRLSTAT(CTRLSTAT_CSYSPWRUPREQ | CTRLSTAT_CDBGPWRUPREQ);

        // Check whether the operation was successful
        final XCom.TimeoutMS tms  = new XCom.TimeoutMS(SWD_INIT_WAIT_PUDBGIFC_MS);

        while( ( _swdRdDP_CTRLSTAT() & (CTRLSTAT_CSYSPWRUPACK | CTRLSTAT_CDBGPWRUPACK) ) != (CTRLSTAT_CSYSPWRUPACK | CTRLSTAT_CDBGPWRUPACK) ) {
            if( tms.timeout() ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_PUDbgIfc, ProgClassName);
        }
    }

    protected void _swd_abort() throws USB2GPIO.TansmitError
    {
        try {
            // Clear error(s)
            _swdWrDP_ABORT(false);
        }
        catch(final Exception e) {
            _swd_reinit();
        }
    }

    public void swdLineReset() throws USB2GPIO.TansmitError
    {
        if( !_usb2gpio.spiTransfer(
            XCom.arrayConcatCopy(_cmdSWDLineReset, _cmdRdDP_DPIDR)
        ) ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_LineReset, ProgClassName);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private synchronized boolean _swd_MemAP_NNbit(final int dt) throws USB2GPIO.TansmitError
    {
        // Configure the transfer size and address increment                           ┌─────→ ADDRINC = 0b01  -> increment single
        //                                                                             ├┐ ┌┬┬→ SIZE    = 0bXXX -> data size
        //                                                           0b11001000    0b00010XXX
        //                                                           ├┐            ├┐
        final long cswNew = _swdRdMemAP(MemAP.CSW        ) & 0xFFFFFFC8L | 0x00000010L | dt;
                            _swdWrMemAP(MemAP.CSW, cswNew);

        return ( _swdRdMemAP(MemAP.CSW) & 0b00110111 ) == (0x00000010L | dt);
    }

    @package_private
    boolean _swd_MemAP_008bit() throws USB2GPIO.TansmitError
    { return _swd_MemAP_NNbit(0b000); }

    @package_private
    boolean _swd_MemAP_016bit() throws USB2GPIO.TansmitError
    { return _swd_MemAP_NNbit(0b001); }

    @package_private synchronized
    boolean _swd_MemAP_032bit() throws USB2GPIO.TansmitError
    { return _swd_MemAP_NNbit(0b010); }

    @package_private synchronized
    boolean _swd_MemAP_064bit() throws USB2GPIO.TansmitError
    { return _swd_MemAP_NNbit(0b011); }

    @package_private synchronized
    boolean _swd_MemAP_128bit() throws USB2GPIO.TansmitError
    { return _swd_MemAP_NNbit(0b100); }

    @package_private synchronized
    boolean _swd_MemAP_256bit() throws USB2GPIO.TansmitError
    { return _swd_MemAP_NNbit(0b101); }

    @package_private synchronized
    void _swdWrBus2(final long address, final long value) throws USB2GPIO.TansmitError
    {
        _swdWrMemAP(MemAP.TAR, address);
        _swdWrMemAP(MemAP.DRW, value  );
        _swdWrMemAP(MemAP.DRW, value  );
    }

    @package_private synchronized
    void _swdWrBus3(final long address, final long value) throws USB2GPIO.TansmitError
    {
        _swdWrMemAP(MemAP.TAR, address);
        _swdWrMemAP(MemAP.DRW, value  );
        _swdWrMemAP(MemAP.DRW, value  );
        _swdWrMemAP(MemAP.DRW, value  );
    }

    @package_private synchronized
    void _swdWrBus4(final long address, final long value) throws USB2GPIO.TansmitError
    {
        _swdWrMemAP(MemAP.TAR, address);
        _swdWrMemAP(MemAP.DRW, value  );
        _swdWrMemAP(MemAP.DRW, value  );
        _swdWrMemAP(MemAP.DRW, value  );
        _swdWrMemAP(MemAP.DRW, value  );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @package_private synchronized
    int _swd_dpVersion() throws USB2GPIO.TansmitError
    { return (_dapVer == null) ? 0 : _dapVer[_idxSelMultidropID]; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @package_private synchronized
    boolean _swd_core_S_LOCKUP() throws USB2GPIO.TansmitError
    { return ( _swdRdBus(_config.cortexMReg.DHCSR) & 0x00080000L ) != 0; }

    @package_private synchronized
    boolean _swd_core_S_SLEEP() throws USB2GPIO.TansmitError
    { return ( _swdRdBus(_config.cortexMReg.DHCSR) & 0x00040000L ) != 0; }

    @package_private synchronized
    boolean _swd_core_S_HALT() throws USB2GPIO.TansmitError
    { return ( _swdRdBus(_config.cortexMReg.DHCSR) & 0x00020000L ) != 0; }

    @package_private synchronized
    boolean _swd_coreIsRunning() throws USB2GPIO.TansmitError
    { return ( _swdRdBus(_config.cortexMReg.DHCSR) & 0x000E0000L ) == 0; }

    @package_private synchronized
    void _swd_haltCore(final boolean reset) throws USB2GPIO.TansmitError
    {
        if(reset) {
            //                                          ┌──────────→ ENDIANESS     = 0b0 -> little endian
            //                                        0b00000000
            //                                        │        ┌───→ SYSRESETREQ   = 0b1 -> request reset
            //                                        │        │┌──→ ECTCLRACTIVE  = 0b0 -> do not clear state information
            //                           VECTKEY ←┐   │ 0b00000100
            //                                    ├┬┬┐├┐├┐
            _swdWrBus(_config.cortexMReg.AIRCR, 0x05FA0004L);
        }
            //                                                ┌────→ C_MASKINTS    = 0b1 -> mask PendSV, SysTick, and external configurable interrupts
            //                                                │┌───→ C_STEP        = 0b0 -> single-stepping disabled
            //                                                ││┌──→ C_HALT        = 0b1 -> request a running processor to halt
            //                            DBGKEY ←┐     0b00001011─→ C_DEBUGEN     = 0b1 -> halting debug enabled
            //                                    ├┬┬┐  ├┐
            _swdWrBus(_config.cortexMReg.DHCSR, 0xA05F0003L);
            _swdWrBus(_config.cortexMReg.DHCSR, 0xA05F000BL);

            //                                                       ARMv6M ARMv7M+             [ARMv7M ] [ARMv8M ]
            //                                    0b00000000───────→ DWTENA/TRCENA = 0b0 -> DWT [and ITM] [and PMU] disabled
            //                                    │          ┌─────→ VC_HARDERR    = 0b0 -> halting debug trap disabled
            //                                    │   0b00000000
            //                                    │   │ 0b00000001─→ VC_CORERESET  = 0b1 -> reset vector catch enabled
            //                                    ├┐  ├┐├┐
            _swdWrBus(_config.cortexMReg.DEMCR, 0x00000001L);
    }

    @package_private synchronized
    void _swd_unhaltCore(final boolean reset, final boolean enableDebug) throws USB2GPIO.TansmitError
    {
        if(reset) _swdWrBus(_config.cortexMReg.AIRCR,                             0x05FA0004L); // Reset the core as needed
                  _swdWrBus(_config.cortexMReg.DHCSR, enableDebug ? 0xA05F0001L : 0xA05F0000L); // Run the core
    }

    protected synchronized void _swd_haltAllCores() throws USB2GPIO.TansmitError
    {
        // Check if there are no multidrop IDs
        if(_multidropIDs == null) {
            _swd_haltCore(false);
            return;
        }

        // Save the index of the currently selected multidrop ID
        final int idxCurMultidropID = _idxSelMultidropID;

        // Unhalt all cores
        for(int idx = 0; idx < _multidropIDs.length; ++idx) {
            _idxSelMultidropID = idx;
            _swd_reinit();
            _swd_haltCore(false);
        }

        // Reselect the previously selected multidrop ID
        _idxSelMultidropID = idxCurMultidropID;
        _swd_reinit();
    }

    protected synchronized boolean _swd_resetAndUnhaltAllCores()
    {
        try {

            // Check if there are no multidrop IDs
            if(_multidropIDs == null)  {
                _swd_unhaltCore(true, false);
                _swd_reinit();
                return true;
            }

            // Save the index of the currently selected multidrop ID
            final int idxCurMultidropID = _idxSelMultidropID;

            // Unhalt all cores
            for(int idx = 0; idx < _multidropIDs.length; ++idx) {
                _idxSelMultidropID = idx;
                _swd_reinit();
                _swd_unhaltCore(true, false);
            }

            // Reselect the previously selected multidrop ID
            _idxSelMultidropID = idxCurMultidropID;
            _swd_reinit();

            // Done
            return true;

        } // try
        catch(final USB2GPIO.TansmitError e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @package_private synchronized
    long _swdRdCoreReg(final int regSel) throws USB2GPIO.TansmitError
    {
        _swdWrBus( _config.cortexMReg.DCRSR, regSel & 0x3F );

        while( ( _swdRdBus( _config.cortexMReg.DHCSR) & 0x00010000L ) == 0 );

        return _swdRdBus(_config.cortexMReg.DCRDR);
    }

    @package_private synchronized
    void _swdWrCoreReg(final int regSel, final long value) throws USB2GPIO.TansmitError
    {
        _swdWrBus( _config.cortexMReg.DCRDR, value                       );
        _swdWrBus( _config.cortexMReg.DCRSR, (1 << 16) | (regSel & 0x3F) );

        while( ( _swdRdBus( _config.cortexMReg.DHCSR) & 0x00010000L ) == 0 );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @package_private synchronized
    long _swdRdCoreMem(final long address) throws USB2GPIO.TansmitError
    {
               _swdWrMemAP(MemAP.TAR, address);
        return _swdRdMemAP(MemAP.DRW         );
    }

    @package_private synchronized
    void _swdRdCoreMem(final long startAddress, final long[] buff, final int transferSize) throws USB2GPIO.TansmitError
    {
        if( (transferSize > 0) && (transferSize < buff.length) && (buff.length % transferSize) != 0 ) {
            USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_NANotMultipleOfB, ProgClassName, "buff.length", "transferSize");
        }

        // Send multiple commands at once
        if(USE_MULTI_CMD_FOR_RD_CORE_MEM) {
                  int tz = (transferSize > 0) ? Math.min(transferSize, buff.length) : buff.length;
            final int nc = buff.length / tz;

            for(int c = 0; c < nc; ++c) {

                int ofs = c * tz;

                _swdWrMemAP(MemAP.TAR, startAddress + ofs);

                while(tz > 0) {

                    final int rdCnt = Math.min(tz, 16);

                    XCom.arrayCopy( buff, ofs, _swdRdMemAP_DRW(rdCnt) );

                    ofs += rdCnt;
                    tz  -= rdCnt;

                } // while

            } // for
        }

        // Send commands individually
        else {
            final int tz = (transferSize > 0) ? Math.min(transferSize, buff.length) : buff.length;
            final int nc = buff.length / tz;

            for(int c = 0; c < nc; ++c) {

                final int ofs = c * tz;

                                                            _swdWrMemAP(MemAP.TAR, startAddress + ofs);
                for(int i = 0; i < tz; ++i) buff[i + ofs] = _swdRdMemAP(MemAP.DRW                    );

            } // for
        }
    }

    @package_private synchronized
    void _swdRdCoreMem(final long startAddress, final long[] buff) throws USB2GPIO.TansmitError
    { _swdRdCoreMem(startAddress, buff, 0); }

    @package_private synchronized
    void _swdRdCoreMem(final long startAddress, final int[] buff, final int count, final int transferSize) throws USB2GPIO.TansmitError
    {
        if( (transferSize > 0) && (transferSize < count) && (count % transferSize) != 0 ) {
            USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_NANotMultipleOfB, ProgClassName, "count", "transferSize");
        }

        // Send multiple commands at once
        if(USE_MULTI_CMD_FOR_RD_CORE_MEM) {
                  int tz = (transferSize > 0) ? Math.min(transferSize, count) : count;
            final int nc = count / tz;

            for(int c = 0; c < nc; ++c) {

                int ofs = c * tz;

                _swdWrMemAP(MemAP.TAR, startAddress + ofs);

                while(tz > 0) {

                    final int    rdCnt  = Math.min(tz / 4, 16);
                    final long[] values = _swdRdMemAP_DRW(rdCnt);

                    for(int i = 0; i < rdCnt; ++i) {

                       final long value = values[i];

                        buff[ofs++] = (int) ( (value >>  0) & 0xFF );
                        buff[ofs++] = (int) ( (value >>  8) & 0xFF );
                        buff[ofs++] = (int) ( (value >> 16) & 0xFF );
                        buff[ofs++] = (int) ( (value >> 24) & 0xFF );

                    } // for

                    tz -= rdCnt * 4;

                } // while

            } // for
        }

        // Send commands individually
        else {
            final int tz = (transferSize > 0) ? Math.min(transferSize, count) : count;
            final int nc = count / tz;

            for(int c = 0; c < nc; ++c) {

                final int ofs = c * tz;

                _swdWrMemAP(MemAP.TAR, startAddress + ofs);

                for(int i = 0; i < tz; i += 4) {

                  //SysUtil.stdDbg().printf("[%d] %08X %08X\n", c, startAddress + ofs + i, startAddress + ofs + count);
                  //SysUtil.stdDbg().printf("[%d] %d\n", c, i + ofs);

                    final long value = _swdRdMemAP(MemAP.DRW);

                    buff[i + ofs + 3] = (int) ( (value >> 24) & 0xFF );
                    buff[i + ofs + 2] = (int) ( (value >> 16) & 0xFF );
                    buff[i + ofs + 1] = (int) ( (value >>  8) & 0xFF );
                    buff[i + ofs + 0] = (int) ( (value >>  0) & 0xFF );

                } // for

            } // for
        }
    }

    @package_private synchronized
    void _swdRdCoreMem(final long startAddress, final int[] buff, final int count) throws USB2GPIO.TansmitError
    { _swdRdCoreMem(startAddress, buff, count, 0); }

    @package_private synchronized
    void _swdRdCoreMem(final long startAddress, final int[] buff) throws USB2GPIO.TansmitError
    { _swdRdCoreMem(startAddress, buff, buff.length, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @package_private synchronized
    void _swdWrCoreMem(final long address, final long value) throws USB2GPIO.TansmitError
    {
        _swdWrMemAP(MemAP.TAR, address);
        _swdWrMemAP(MemAP.DRW, value  );
    }

    @package_private synchronized
    void _swdWrCoreMem(final long startAddress, final long[] buff, final int transferSize) throws USB2GPIO.TansmitError
    {
        if( (transferSize > 0) && (transferSize < buff.length) && (buff.length % transferSize) != 0 ) {
            USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_NANotMultipleOfB, ProgClassName, "buff.length", "transferSize");
        }

        // Send multiple commands at once
        if(USE_MULTI_CMD_FOR_WR_CORE_MEM) {
                  int tz = (transferSize > 0) ? Math.min(transferSize, buff.length) : buff.length;
            final int nc = buff.length / tz;

            for(int c = 0; c < nc; ++c) {

                int ofs = c * tz;

                _swdWrMemAP(MemAP.TAR, startAddress + ofs);

                while(tz > 0) {

                    final int wrCnt = Math.min(tz, 16);

                    _swdWrMemAP_DRW( XCom.arrayCopy(buff, ofs, wrCnt) );

                    ofs += wrCnt;
                    tz  -= wrCnt;

                } // while

            } // for
        }

        // Send commands individually
        else {
            final int tz = (transferSize > 0) ? Math.min(transferSize, buff.length) : buff.length;
            final int nc = buff.length / tz;

            for(int c = 0; c < nc; ++c) {

                final int ofs = c * tz;

                                            _swdWrMemAP(MemAP.TAR, startAddress + ofs );
                for(int i = 0; i < tz; ++i) _swdWrMemAP(MemAP.DRW, buff[i + ofs]      );

            } // for
        }
    }

    @package_private synchronized
    void _swdWrCoreMem(final long startAddress, final long[] buff) throws USB2GPIO.TansmitError
    { _swdWrCoreMem(startAddress, buff, 0); }

    @package_private synchronized
    void _swdWrCoreMem(final long startAddress, final int[] buff, final int count, final int transferSize) throws USB2GPIO.TansmitError
    {
        if( (transferSize > 0) && (transferSize < count) && (count % transferSize) != 0 ) {
            USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_NANotMultipleOfB, ProgClassName, "count", "transferSize");
        }

        // Send multiple commands at once
        if(USE_MULTI_CMD_FOR_WR_CORE_MEM) {
                  int tz = (transferSize > 0) ? Math.min(transferSize, count) : count;
            final int nc = count / tz;

            for(int c = 0; c < nc; ++c) {

                int ofs = c * tz;

                _swdWrMemAP(MemAP.TAR, startAddress + ofs);

                while(tz > 0) {

                    final int    wrCnt  = Math.min(tz / 4, 16);
                    final long[] values = new long[wrCnt];

                    for(int i = 0; i < wrCnt; ++i) {

                       values[i] = ( ( (long) buff[ofs + 3] ) << 24 )
                                 | ( ( (long) buff[ofs + 2] ) << 16 )
                                 | ( ( (long) buff[ofs + 1] ) <<  8 )
                                 | ( ( (long) buff[ofs + 0] ) <<  0 );

                       ofs += 4;

                    } // for

                    _swdWrMemAP_DRW(values);

                    tz -= wrCnt * 4;

                } // while

            } // for
        }

        // Send commands individually
        else {
            final int tz = (transferSize > 0) ? Math.min(transferSize, count) : count;
            final int nc = count / tz;

            for(int c = 0; c < nc; ++c) {

                final int ofs = c * tz;

                _swdWrMemAP(MemAP.TAR, startAddress + ofs);

                for(int i = 0; i < tz; i += 4) {

                    final long value = ( ( (long) buff[i + ofs + 3] ) << 24 )
                                     | ( ( (long) buff[i + ofs + 2] ) << 16 )
                                     | ( ( (long) buff[i + ofs + 1] ) <<  8 )
                                     | ( ( (long) buff[i + ofs + 0] ) <<  0 );

                    _swdWrMemAP(MemAP.DRW, value);

                } // for

            } // for
        }
    }

    @package_private synchronized
    void _swdWrCoreMem(final long startAddress, final int[] buff, final int count) throws USB2GPIO.TansmitError
    { _swdWrCoreMem(startAddress, buff, count, 0); }

    @package_private synchronized
    void _swdWrCoreMem(final long startAddress, final int[] buff) throws USB2GPIO.TansmitError
    { _swdWrCoreMem(startAddress, buff, buff.length, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean _swdIsFlashLocked(final SWDFlashLoader.Specifier flSpec) throws USB2GPIO.TansmitError
    {
        if(flSpec == null || flSpec.instruction_IsFlashLocked == null) return false;

        return _swdExecInst._exec(flSpec.instruction_IsFlashLocked) != 0;
    }

    protected boolean _swdIsFlashLocked() throws USB2GPIO.TansmitError
    {
        // Get the flash loader specification and unlock the flash using the flash loader if it exists
        final SWDFlashLoader.Specifier flSpec = SWDFlashLoader.getSpecifierFor(_config, _swdExecInst);

        return _swdIsFlashLocked(flSpec);
    }

    protected void _swdUnlockFlash(final SWDFlashLoader.Specifier flSpec) throws USB2GPIO.TansmitError
    { if(flSpec != null && flSpec.instruction_UnlockFlash != null) _swdExecInst._exec(flSpec.instruction_UnlockFlash); }

    protected void _swdUnlockFlash() throws USB2GPIO.TansmitError
    {
        // Get the flash loader specification and unlock the flash using the flash loader if it exists
        final SWDFlashLoader.Specifier flSpec = SWDFlashLoader.getSpecifierFor(_config, _swdExecInst);

        if(flSpec != null) _swdUnlockFlash(flSpec);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean _swdIsEEPROMLocked(final SWDFlashLoader.Specifier flSpec) throws USB2GPIO.TansmitError
    {
        if(flSpec == null || flSpec.instruction_IsEEPROMLocked == null) return false;

        return _swdExecInst._exec(flSpec.instruction_IsEEPROMLocked) != 0;
    }

    protected boolean _swdIsEEPROMLocked() throws USB2GPIO.TansmitError
    {
        // Get the flash loader specification and unlock the EEPROM using the flash loader if it exists
        final SWDFlashLoader.Specifier flSpec = SWDFlashLoader.getSpecifierFor(_config, _swdExecInst);

        return _swdIsEEPROMLocked(flSpec);
    }

    protected void _swdUnlockEEPROM(final SWDFlashLoader.Specifier flSpec) throws USB2GPIO.TansmitError
    { if(flSpec != null && flSpec.instruction_UnlockEEPROM != null) _swdExecInst._exec(flSpec.instruction_UnlockEEPROM); }

    protected void _swdUnlockEEPROM() throws USB2GPIO.TansmitError
    {
        // Get the flash loader specification and unlock the EEPROM using the flash loader if it exists
        final SWDFlashLoader.Specifier flSpec = SWDFlashLoader.getSpecifierFor(_config, _swdExecInst);

        if(flSpec != null) _swdUnlockEEPROM(flSpec);
    }

} // class ProgSWDLowLevel
